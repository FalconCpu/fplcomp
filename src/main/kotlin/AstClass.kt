package falcon

class AstClass(
    location: Location,
    parent: AstBlock,
    private val name: String,
    private val parameters: List<AstParameter>,
    private val astSuperClass: AstSuperClass?,
    val isAbstract : Boolean
) : AstBlock(location, parent) {

    lateinit var constructorParameters : List<Symbol>
    val superClass = resolveSuperClass(parent)
    private val type = makeClassType(name, this, superClass)
    private val symbol = SymbolTypeName(location, name, type)

    init {
        parent.add(symbol)
    }

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CLASS $name\n")
        for (parameter in parameters)
            parameter.dump(sb, indent + 1)
        astSuperClass?.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CLASS $name\n")
        for (parameter in parameters)
            parameter.dumpWithType(sb, indent + 1)
        astSuperClass?.dumpWithType(sb, indent + 1)
        for (stmt in body)
            stmt.dumpWithType(sb, indent + 1)
    }

    private fun checkArgs(params:List<Type>, args:List<AstExpr>) {
        val argTypes = args.map { it.type }
        if (params.size != argTypes.size)
            return Log.error(astSuperClass!!.location, "Got ${argTypes.size} arguments when expecting ${params.size}")
        for (index in args.indices) {
            params[index].checkAssignCompatible(args[index].location, argTypes[index])
        }
    }

    override fun identifyFunctions(context: AstBlock) {
        // resolve the types of the parameters and add them to the symbol table
        constructorParameters = parameters.map { it.resolveParameter(context) }
        constructorParameters.forEach { add(it) }

        // If we have a superclass then check the arguments to the super class constructor
        if (superClass!=null) {
            val superclassConstructorArgs = astSuperClass!!.args
            superclassConstructorArgs.forEach { it.typeCheck(this) }
            val superclassParams = superClass.definition.constructorParameters.map { it.type }
            checkArgs(superclassParams, astSuperClass.args)

            // copy all the members from the super class into the current class
            symbolTable.clear()
            for(sym in superClass.definition.symbolTable.values)
                add(sym)

            // and copy the constructor parameters back into the symbol table
            // If our constructor has a local variable with the same name as superclass member then drop it
            for(param in constructorParameters)
                if (symbolTable[param.name]==null || param !is SymbolLocalVar)
                    add(param)
        }

        // Resolve any other fields in the class
        for (stmt in body)
            if (stmt is AstDeclaration)
                stmt.typeCheck(this)
            else if (stmt is AstBlock)
                stmt.identifyFunctions(this)
    }

    private fun resolveSuperClass(context: AstBlock) : ClassType? {
        if (astSuperClass == null)
            return null

        val symbol = predefinedSymbols.lookup(astSuperClass.name) ?: context.lookup(astSuperClass.name)
        if (symbol == null) {
            Log.error(astSuperClass.location, "Unknown super class ${astSuperClass.name}")
            return null
        }
        if (symbol !is SymbolTypeName || symbol.type !is ClassType) {
            Log.error(astSuperClass.location, "Super class ${astSuperClass.name} is not a class")
            return null
        }

        return symbol.type
    }

    override fun typeCheck(context: AstBlock) {
        for(statement in body)
            if (statement !is AstDeclaration)
                statement.typeCheck(this)

        if (!isAbstract) {
            symbolTable.values.filterIsInstance<SymbolFunctionName>()
                .filter { it.astFunction.methodKind == MethodKind.ABSTRACT_METHOD }
                .forEach { Log.error(it.location, "No override provided for abstract function '$it'") }
        }
    }
}
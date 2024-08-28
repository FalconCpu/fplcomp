package frontend

class AstClass(
    location: Location,
    parent: AstBlock,
    val name: String,
    private val parameters: List<AstParameter>,
    private val astSuperClass: AstSuperClass?,
    val isAbstract : Boolean
) : AstBlock(location, parent) {

    val superClass = resolveSuperClass(parent)
    val type = makeClassType(name, this, superClass)
    val symbol = SymbolTypeName(location, name, type)
    lateinit var constructorParameters: List<Symbol>    // Filled in at identifyFunctions()
    lateinit var tcClass : TcClass

    init {
        parent.add(symbol)
    }

    override fun toString() = name

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CLASS $name\n")
        for (parameter in parameters)
            parameter.dump(sb, indent + 1)
        astSuperClass?.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun identifyFunctions(context: AstBlock) {
        // resolve the types of the parameters and add them to the symbol table
        constructorParameters = parameters.map { it.resolveParameter(context) }
        constructorParameters.forEach { add(it) }

        // If we have a superclass then check the arguments to the super class constructor

        if (superClass!=null) {
            val superclassConstructorArgs = astSuperClass!!.resolveArgs(this)
            val superclassParams = superClass.definition.constructorParameters
            checkArgListSymbol(location, superclassParams, superclassConstructorArgs)

            // import all the members from the super class into the current class
            import(superClass.definition)

            // add our constructor parameters back into the symbol table, but allow for the case where
            // we have a parameter with the same name as a superclass field
            for(param in constructorParameters)
                if (lookupNoHierarchy(param.name)==null || param !is SymbolLocalVar)
                    add(param)

            tcClass = TcClass(location, name, constructorParameters,
                superClass.definition.type, superclassConstructorArgs, isAbstract)

        } else {
            tcClass = TcClass(location, name, constructorParameters,
                null, emptyList(), isAbstract)
        }

        // Resolve any other fields in the class and complete building the constructor
        for (stmt in body)
            when (stmt) {
                is AstDeclaration -> tcClass.add( stmt.typeCheck(this) )
                is AstBlock -> stmt.identifyFunctions(this)
                else -> error("Unexpected statement in class ${stmt.javaClass}")
            }

        // Local variables are only accessible in the constructor.
        // Since we have finished building the constructor now remove them from the symbol table to
        // make sure they don't get used in any methods
        removeLocals()
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

    override fun typeCheck(context: AstBlock) : TcClass {
        for(statement in body)
            if (statement !is AstDeclaration)
                tcClass.add( statement.typeCheck(this) )

        if (!isAbstract) {
            getMethods()
                .forEach{ if (it.methodKind == MethodKind.ABSTRACT_METHOD)
                    Log.error(it.location, "No override provided for abstract function '$it'")
                }
        }
        return tcClass
    }

}

class TcClass(
    location: Location,
    val name: String,
    private val parameters: List<Symbol>,
    private val superClass: ClassType?,
    private val superClassConstructorArgs: List<TcExpr>,
    val isAbstract : Boolean
) : TcBlock(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CLASS $name\n")
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun codeGen() {
        TODO("Not yet implemented")
    }
}

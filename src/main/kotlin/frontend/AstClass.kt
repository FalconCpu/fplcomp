package frontend

import backend.Function
import backend.InstrEnd
import backend.InstrStart
import backend.allMachineRegs

class AstClass(
    location: Location,
    parent: AstBlock,
    val name: String,
    private val parameters: List<AstParameter>,
    private val superClassName: AstIdentifier?,
    private val superClassConstructorArgs: List<AstExpr>,
    val isAbstract : Boolean
) : AstBlock(location, parent) {

    val superClass = resolveSuperClass(parent)
    val type = makeClassType(name, this, superClass, isAbstract)
    val symbol = SymbolTypeName(location, name, type)
    lateinit var tcClass: TcClass

    init {
        parent.add(symbol)
    }

    override fun toString() = name

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CLASS $name\n")
        for (parameter in parameters)
            parameter.dump(sb, indent + 1)
        if (superClassName != null) {
            sb.append(". ".repeat(indent + 1))
            sb.append("${superClassName.name}\n")
            superClassConstructorArgs.forEach { it.dump(sb, indent + 2) }
        }
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun add(symbol: Symbol) {
        // add a symbol to the symbol table. Allow fields to override local variables

        val duplicate = symbolTable[symbol.name]
        if (duplicate != null && !(duplicate is SymbolLocalVar && symbol is SymbolField))
            Log.error(symbol.location, "duplicate symbol: $symbol   ${symbol.javaClass} ${duplicate.javaClass}")
        symbolTable[symbol.name] = symbol

        // Add fields and methods to the class type
        if (symbol is SymbolField) {
            if (symbol.offset==Symbol.UNDEFINED_OFFSET)
                symbol.offset = type.fields.size
            else if (symbol.offset != type.fields.size)
                Log.error(symbol.location, "Conflict on offset : $symbol: $type ${symbol.offset} ${type.fields.size}")
            type.fields += symbol
        }

        if (symbol is SymbolFunctionName) {
            if (symbol.funcNo==Symbol.UNDEFINED_OFFSET)
                symbol.funcNo = type.methods.size
            else if (symbol.funcNo != type.methods.size)
                Log.error(symbol.location, "Conflict on offset : $symbol $type ${symbol.funcNo} ${type.methods.size}")
            println("Adding method $symbol to $type  offset ${symbol.funcNo} ${type.methods.size}")
            type.methods += symbol

        }
    }


    override fun identifyFunctions(context: AstBlock) {
        // resolve the types of the parameters. Add local variables to the symbol table
        type.constructorParameters = parameters.map { it.resolveParameter(context) }
        type.constructorParameters.filterIsInstance<SymbolLocalVar>().forEach { add(it) }
        val thisSymbol = SymbolLocalVar(location, "this", type, false)
        add(thisSymbol)

        // If we have a superclass then check the arguments to the super class constructor
        val superclassConstructorArgs = superClassConstructorArgs.map { it.typeCheck(this) }
        if (superClass != null) {
            val superclassParams = superClass.constructorParameters
            checkArgListSymbol(location, superclassParams, superclassConstructorArgs)

            // import all fields and methods from the super class into the current class
            println(superClass.fields)
            superClass.fields.forEach { add(it) }
            superClass.methods.forEach { add(it) }
        }

        tcClass = TcClass(
            location, name, type, thisSymbol,
            superClass, superclassConstructorArgs
        )

        // Add the rest of our parameters to the symbol table
        type.constructorParameters.forEach { if (it !is SymbolLocalVar) add(it) }

        // Resolve any other fields in the class and complete building the constructor
        for (stmt in body)
            when (stmt) {
                is AstDeclaration -> tcClass.add(stmt.typeCheck(this))
                is AstBlock -> stmt.identifyFunctions(this)
                else -> error("Unexpected statement in class ${stmt.javaClass}")
            }

        // Local variables are only accessible in the constructor.
        // Since we have finished building the constructor now remove them from the symbol table to
        // make sure they don't get used in any methods
        removeLocals()

        // Determine the offsets for all the fields
        val fields = getFields()
        for ((index, field) in fields.withIndex()) {
            field.offset = index
        }
    }

    private fun resolveSuperClass(context: AstBlock) : ClassType? {
        if (superClassName == null)
            return null

        val symbol = predefinedSymbols.lookup(superClassName.name) ?: context.lookup(superClassName.name)
        if (symbol == null) {
            Log.error(superClassName.location, "Unknown super class ${superClassName.name}")
            return null
        }
        if (symbol !is SymbolTypeName || symbol.type !is ClassType) {
            Log.error(superClassName.location, "Super class ${superClassName.name} is not a class")
            return null
        }

        return symbol.type
    }

    override fun typeCheck(context: AstBlock) : TcClass {
        for (statement in body)
            if (statement !is AstDeclaration)
                tcClass.add(statement.typeCheck(this))

        if (!isAbstract) {
            getMethods()
                .forEach {
                    if (it.methodKind == MethodKind.ABSTRACT_METHOD)
                        Log.error(it.location, "No override provided for abstract function '$it'")
                }
        }
        return tcClass
    }
}

class TcClass(
    location: Location,
    val name: String,
    val type: ClassType,
    private val thisSymbol : SymbolLocalVar,
    private val superClass: ClassType?,
    private val superClassConstructorArgs: List<TcExpr>,
) : TcBlock(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CLASS $name\n")
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun codeGen() {
        val oldCurrentFunction = currentFunction
        type.constructor = Function(name)
        currentFunction = type.constructor

        currentFunction.add(InstrStart())

        // Load the parameters into registers/fields
        var argno = 1
        currentFunction.thisReg = currentFunction.getReg(thisSymbol)
        currentFunction.instrMove( currentFunction.getThis(), allMachineRegs[argno++])

        for (param in type.constructorParameters)
            when (param) {
                is SymbolLocalVar -> currentFunction.instrMove( currentFunction.getReg(param), allMachineRegs[argno++])
                is SymbolField -> currentFunction.instrStore(allMachineRegs[argno++], currentFunction.getThis(), param )
                else -> error("Invalid type ${param.javaClass} is constructor")
            }

        // Call the superclass constructor
        if (superClass!=null) {
            argno = 1
            currentFunction.instrMove( allMachineRegs[argno++], currentFunction.getThis())
            val args = superClassConstructorArgs.map {it.codeGenRvalue()}
            for(arg in args)
                currentFunction.instrMove( allMachineRegs[argno++], arg)
            currentFunction.instrCall(superClass.constructor)
        }

        for (statement in body)
            statement.codeGen()

        currentFunction.instrLabel(currentFunction.endLabel)
        currentFunction.add(InstrEnd())

        currentFunction = oldCurrentFunction
    }
}

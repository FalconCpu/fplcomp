package frontend

import backend.*

lateinit var enclosingFunction: TcFunction

class AstFunction (
    location: Location,
    parent : AstBlock,
    private val name: String,
    private val astParams: List<AstParameter>,
    private val returnType: AstType?,
    val methodKind: MethodKind,
    private val methodOf : AstClass?
) : AstBlock(location, parent) {

    var endLocation = nullLocation   // value gets poked in by Parser
    lateinit var tcFunction: TcFunction

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("FUNCTION $name\n")
        for (arg in astParams)
            arg.dump(sb, indent + 1)
        returnType?.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }


    private fun checkOverride(symbol: SymbolFunctionName,   function: TcFunction, methodOf: ClassType) {
        val paramTypes = function.params.map { it.type }
        println("In checkOverride: ${symbol.overloads}")
        val superclassMethod = symbol.overloads.find { it.exactMatchParams(paramTypes) }
        if (superclassMethod == null)
            return Log.error(location, "No function to override")
        if (superclassMethod.methodKind == MethodKind.NONE)
            Log.error(location, "Cannot override closed method")

        function.methodId = superclassMethod.methodId
        methodOf.methods[ function.methodId ] = function
        symbol.overloads.replaceAll { if (it == superclassMethod) function else it }
    }

    private fun addFunctionToSymbol(symbol: SymbolFunctionName, function: TcFunction, methodOf: ClassType?) {
        val paramTypes = function.params.map { it.type }
        val duplicate = symbol.overloads.find { it.exactMatchParams(paramTypes) }
        if (duplicate != null)
            return Log.error(location, "Duplicate overload")
        if (methodOf!=null) {
            function.methodId = methodOf.methods.size
            methodOf.methods += function
        }
        symbol.overloads.add(function)
    }

    override fun identifyFunctions(context: AstBlock) {
        val params = astParams.map { it.resolveParameter(context) }
        val paramTypes = params.map { it.type }
        val retType = returnType?.resolveType(context) ?: UnitType

        val symbol = context.lookupNoHierarchy(name) ?: run {
            SymbolFunctionName(location, name). also {context.add(it) }
        }

        if (symbol !is SymbolFunctionName)
            return Log.error(location, "Conflict between variable and function name")


        for(param in params)
            add(param)

        val thisSymbol =
            if (methodOf!=null)
                SymbolLocalVar(location,"this",methodOf.type, false)
            else
                null

        val nameWithTypes = name+paramTypes.joinToString(separator = ",", prefix = "(", postfix = ")")
        val funcName = if (methodOf!=null) "$methodOf/$nameWithTypes" else nameWithTypes

        tcFunction = TcFunction(location, funcName, params, retType, methodKind, thisSymbol)

        if (methodKind==MethodKind.OVERRIDE_METHOD)
            checkOverride(symbol, tcFunction, methodOf!!.type)
        else
            addFunctionToSymbol(symbol, tcFunction, methodOf?.type)
    }

    override fun typeCheck(context: AstBlock) : TcFunction {
        enclosingFunction = tcFunction
        currentPathContext = PathContext()
        for(statement in body)
            tcFunction.add( statement.typeCheck(this) )
        if (currentPathContext.isReachable && tcFunction.returnType != UnitType && methodKind!=MethodKind.ABSTRACT_METHOD)
            Log.error(endLocation, "Function $name should return a value of type ${tcFunction.returnType}")
        return tcFunction
    }
}

enum class MethodKind {
    NONE,
    OPEN_METHOD,
    OVERRIDE_METHOD,
    ABSTRACT_METHOD
}

class TcFunction (
    location: Location,
    val name: String,
    val params: List<Symbol>,
    val returnType: Type,
    val methodKind: MethodKind,
    val thisSymbol : SymbolLocalVar?
) : TcBlock(location) {

    val backendFunction = Function(name)
    var methodId = 0

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("FUNCTION $name->$returnType\n")
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun toString() = name

    fun matchParams(args:List<Type>) : Boolean {
        if (args.size != params.size)
            return false

        for (i in args.indices)
            if (!params[i].type.isAssignableFrom(args[i]))
                return false
        return true
    }

    fun exactMatchParams(args:List<Type>) : Boolean {
        if (args.size != params.size)
            return false

        for (i in args.indices)
            if (params[i].type != args[i])
                return false
        return true
    }


    override fun codeGen() {
        val oldCurrentFunction = currentFunction
        currentFunction = backendFunction

        currentFunction.add(InstrStart())

        var argno = 1
        if (thisSymbol!=null) {
            currentFunction.thisReg = currentFunction.getReg(thisSymbol)
            currentFunction.instrMove( currentFunction.getThis(), allMachineRegs[argno++])
        }

        for (param in params)
            currentFunction.instrMove( currentFunction.getReg(param), allMachineRegs[argno++])

        for (statement in body)
            statement.codeGen()

        currentFunction.instrLabel(currentFunction.endLabel)
        currentFunction.add(InstrEnd())

        currentFunction = oldCurrentFunction
    }
}


fun checkArgListSymbol(location: Location, params:List<Symbol>, args:List<TcExpr>) {
    val argTypes = args.map { it.type }
    if (params.size != argTypes.size)
        return Log.error(location, "Got ${argTypes.size} arguments when expecting ${params.size}")
    for (index in args.indices) {
        params[index].type.checkAssignCompatible(args[index].location, argTypes[index])
    }
}


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


    private fun checkOverride(context: AstBlock, newSymbol: SymbolFunctionName) {
        check(methodOf!=null)
        val overridenSymbol = methodOf.lookupNoHierarchy(newSymbol.name) ?:
            return Log.error(location, "No method to override")
        if (overridenSymbol !is SymbolFunctionName)
            return Log.error(location, "Cannot override non-function")
        if (overridenSymbol.methodKind == MethodKind.NONE)
            return Log.error(location, "Cannot override closed method")
        if (overridenSymbol.type != newSymbol.type)
            return Log.error(location, "Type mismatch in overriding method. Got ${overridenSymbol.type} but expected ${newSymbol.type}")
        context.replace(newSymbol)
    }

    override fun identifyFunctions(context: AstBlock) {
        val params = astParams.map { it.resolveParameter(context) }
        val retType = returnType?.resolveType(context) ?: UnitType

        val funcType = makeFunctionType(params.map{it.type}, retType)
        val symbol = SymbolFunctionName(location, name, funcType, methodKind)

        if (methodKind==MethodKind.OVERRIDE_METHOD)
            checkOverride(context, symbol)
        else
            context.add(symbol)

        for(param in params)
            add(param)

        val thisSymbol =
            if (methodOf!=null)
                SymbolLocalVar(location,"this",methodOf.type, false)
            else
                null

        val funcName = if (methodOf!=null) "$methodOf/$name" else name

        tcFunction = TcFunction(location, symbolTable, funcName, params, retType, methodKind, thisSymbol, symbol)
        symbol.function = tcFunction
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
    symbolTable: SymbolTable,
    val name: String,
    private val params: List<Symbol>,
    val returnType: Type,
    val methodKind: MethodKind,
    private val thisSymbol : SymbolLocalVar?,
    private val functionSymbol : SymbolFunctionName
) : TcBlock(location, symbolTable) {

    val backendFunction = Function(name)

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("FUNCTION $name ${functionSymbol.type}\n")
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun codeGen() {
        val oldCurrentFunction = currentFunction
        currentFunction = backendFunction

        currentFunction.add(InstrStart())

        for ((index,param) in params.withIndex())
            currentFunction.instrMove( currentFunction.getReg(param), allMachineRegs[index+1])

        for (statement in body)
            statement.codeGen()

        currentFunction.instrLabel(currentFunction.endLabel)
        currentFunction.add(InstrEnd())

        currentFunction = oldCurrentFunction
    }
}

fun checkArgList(location: Location, params:List<Type>, args:List<TcExpr>) {
    val argTypes = args.map { it.type }
    if (params.size != argTypes.size)
        return Log.error(location, "Got ${argTypes.size} arguments when expecting ${params.size}")
    for (index in args.indices) {
        params[index].checkAssignCompatible(args[index].location, argTypes[index])
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


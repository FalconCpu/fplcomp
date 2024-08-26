package frontend

import backend.*

class AstFunction (
    location: Location,
    parent : AstBlock,
    private val name: String,
    private val astParams: List<AstParameter>,
    private val returnType: AstType?,
    val methodKind: MethodKind,
    private val methodOf : AstClass?
) : AstBlock(location, parent) {

    lateinit var params : List<Symbol>
    lateinit var endLocation : Location
    lateinit var symbol: SymbolFunctionName
    lateinit var retType: Type
    lateinit var endLabel: Label


    val backendFunction = backend.Function(
        if (parent is AstClass) "${parent.name}/$name" else name
    )

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("FUNCTION $name\n")
        for (arg in astParams)
            arg.dump(sb, indent + 1)
        returnType?.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("FUNCTION $name ${symbol.type}\n")
        for (stmt in body)
            stmt.dumpWithType(sb, indent + 1)
    }

    private fun checkOverride(context: AstBlock, newSymbol: SymbolFunctionName) {
        check(methodOf!=null)
        val overridenSymbol = methodOf.lookupNoHierarchy(newSymbol.name) ?:
            return Log.error(location, "No method to override")
        if (overridenSymbol !is SymbolFunctionName)
            return Log.error(location, "Cannot override non-function")
        if (overridenSymbol.astFunction.methodKind == MethodKind.NONE)
            return Log.error(location, "Cannot override closed method")
        if (overridenSymbol.type != newSymbol.type)
            return Log.error(location, "Type mismatch in overriding method. Got ${overridenSymbol.type} but expected ${newSymbol.type}")
        context.replace(newSymbol)
    }

    override fun identifyFunctions(context: AstBlock) {
        params = astParams.map { it.resolveParameter(context) }
        retType = returnType?.resolveType(context) ?: UnitType
        val funcType = makeFunctionType(params.map{it.type}, retType)
        symbol = SymbolFunctionName(location, name, funcType, this)

        if (methodKind==MethodKind.OVERRIDE_METHOD)
            checkOverride(context, symbol)
        else
            context.add(symbol)

        for(param in params)
            add(param)
    }

    override fun typeCheck(context: AstBlock) {
        currentPathContext = PathContext()
        for(statement in body)
            statement.typeCheck(this)
        if (currentPathContext.isReachable && retType != UnitType && methodKind!=MethodKind.ABSTRACT_METHOD)
            Log.error(endLocation, "backend.Function should return a value of type $retType")
    }

    override fun codeGen() {
        val oldCurrentFunction = currentFunction
        currentFunction = backendFunction

        currentFunction.add(InstrStart())
        endLabel = currentFunction.newLabel()

        for ((index,param) in params.withIndex())
            currentFunction.instrMove( currentFunction.getReg(param), allMachineRegs[index+1])

        for (statement in body)
            statement.codeGen()

        currentFunction.instrLabel(endLabel)
        currentFunction.add(InstrEnd())

        currentFunction = oldCurrentFunction
    }
}

enum class MethodKind {
    NONE,
    OPEN_METHOD,
    OVERRIDE_METHOD,
    ABSTRACT_METHOD
}
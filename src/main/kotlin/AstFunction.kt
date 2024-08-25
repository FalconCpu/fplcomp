package falcon

class AstFunction (
    location: Location,
    parent : AstBlock,
    private val name: String,
    private val astParams: List<AstParameter>,
    private val returnType: AstType?
) : AstBlock(location, parent) {

    lateinit var endLocation : Location
    lateinit var symbol: SymbolFunctionName
    lateinit var retType: Type

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


    override fun identifyFunctions(context: AstBlock) {
        val params = astParams.map { it.resolveParameter(context) }
        retType = returnType?.resolveType(context) ?: UnitType
        val funcType = makeFunctionType(params.map{it.type}, retType)
        symbol = SymbolFunctionName(location, name, funcType)
        context.add(symbol)
        for(param in params)
            add(param)
    }

    override fun typeCheck(context: AstBlock) {
        currentPathContext = PathContext()
        for(statement in body)
            statement.typeCheck(this)
        if (currentPathContext.isReachable && retType != UnitType)
            Log.error(endLocation, "Function should return a value of type $retType")
    }

}
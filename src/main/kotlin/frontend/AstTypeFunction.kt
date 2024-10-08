package frontend

class AstTypeFunction(
    location: Location,
    private val params: List<AstType>,
    private val retType: AstType?
) : AstType(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("TYPEFUNCTION\n")
        for (param in params)
            param.dump(sb, indent + 1)
        retType?.dump(sb, indent + 1)
    }

    override fun resolveType(context: AstBlock): Type {
        val paramTypes = params.map { it.resolveType(context) }
        val retType = retType?.resolveType(context) ?: UnitType
        return makeFunctionType(paramTypes, retType)
    }
}
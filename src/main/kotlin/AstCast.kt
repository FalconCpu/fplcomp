package falcon

class AstCast(
    location: Location,
    private val expr: AstExpr,
    private val astType: AstType
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CAST\n")
        expr.dump(sb, indent + 1)
        astType.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CAST $type\n")
        expr.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        expr.typeCheck(context)
        type = astType.resolveType(context)
    }

}
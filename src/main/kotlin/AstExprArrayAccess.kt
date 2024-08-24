package falcon

class AstExprArrayAccess(
    location: Location,
    private val lhs: AstExpr,
    private val index: AstExpr
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYACCESS\n")
        lhs.dump(sb, indent + 1)
        index.dump(sb, indent + 1)
    }
}
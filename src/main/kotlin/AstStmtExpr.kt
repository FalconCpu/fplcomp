package falcon

class AstStmtExpr(
    location: Location,
    private val expr: AstExpr
) : AstStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("EXPR\n")
        expr.dump(sb, indent + 1)
    }
}
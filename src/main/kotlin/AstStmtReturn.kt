package falcon

class AstStmtReturn(
    location: Location,
    private val value: AstExpr?
) : AstStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("RETURN\n")
        value?.dump(sb, indent + 1)
    }
}
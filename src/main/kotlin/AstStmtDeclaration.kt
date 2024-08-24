package falcon

class AstStmtDeclaration (
    location: Location,
    private val op : TokenKind,
    private val name: String,
    private val type: AstType?,
    private val value: AstExpr?
) : AstStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("$op $name\n")
        type?.dump(sb, indent + 1)
        value?.dump(sb, indent + 1)
    }
}
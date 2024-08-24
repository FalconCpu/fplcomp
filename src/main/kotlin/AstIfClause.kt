package falcon

class AstIfClause(
    location: Location,
    val condition: AstExpr?,
) : AstBlock(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CLAUSE\n")
        condition?.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }
}
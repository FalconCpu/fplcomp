package falcon

class AstIfClause(
    location: Location,
    parent : AstBlock,
    val condition: AstExpr?,
) : AstBlock(location, parent) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CLAUSE\n")
        condition?.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun typeCheck(context: TcBlock) {
        TODO("Not yet implemented")
    }
}
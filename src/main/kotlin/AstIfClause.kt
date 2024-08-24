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

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CLAUSE\n")
        condition?.dumpWithType(sb, indent + 1)
        for (stmt in body)
            stmt.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context:AstBlock) {
        TODO("Not yet implemented")
    }
}
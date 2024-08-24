package falcon

class AstStmtIf(
    location: Location,
    parent : AstBlock,
    private val clauses : List<AstIfClause>
) : AstBlock(location, parent) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IF\n")
        for (clause in clauses)
            clause.dump(sb, indent + 1)
    }

    override fun typeCheck(context: TcBlock) {
        TODO("Not yet implemented")
    }

}
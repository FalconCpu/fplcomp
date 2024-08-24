package falcon

class AstBlockIf(
    location: Location,
    private val clauses : List<AstIfClause>
) : AstBlock(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IF\n")
        for (clause in clauses)
            clause.dump(sb, indent + 1)
    }
}
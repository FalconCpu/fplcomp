package falcon

class AstBlockTop() : AstBlock(nullLocation) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("TOP\n")
        for (statement in body)
            statement.dump(sb, indent + 1)
    }

    fun dump() : String {
        val sb = StringBuilder()
        dump(sb, 0)
        return sb.toString()
    }
}

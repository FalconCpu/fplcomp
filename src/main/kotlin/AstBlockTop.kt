package falcon

class AstBlockTop() : AstBlock(nullLocation, null) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("TOP\n")
        for (statement in body)
            statement.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("TOP\n")
        for (statement in body)
            statement.dumpWithType(sb, indent + 1)
    }

    fun dump() : String {
        val sb = StringBuilder()
        dump(sb, 0)
        return sb.toString()
    }

    fun dumpWithType() : String {
        val sb = StringBuilder()
        dumpWithType(sb, 0)
        return sb.toString()
    }

    override fun typeCheck(context: AstBlock) {
        for(statement in body)
            statement.typeCheck(this)
    }

    fun typeCheck()  {
        typeCheck(this)
    }

}

package falcon

class AstBlockTop() : AstBlock(nullLocation, null) {

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

    override fun typeCheck(context: TcBlock) {
        TODO("Not yet implemented")
    }

    fun typeCheck() : TcTop {
        val ret = TcTop(symbolTable)
        for(statement in body)
            statement.typeCheck(ret)
        return ret
    }

}

package frontend

import backend.allFunctions

var currentFunction = backend.Function("<dummy>")

class AstTop : AstBlock(nullLocation, null) {

    lateinit var tcTop: TcTop

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

    override fun identifyFunctions(context: AstBlock) {
        tcTop = TcTop()
        for (stmt in body)
            if (stmt is AstBlock)
                stmt.identifyFunctions(this)

    }

    override fun typeCheck(context: AstBlock) : TcTop {
        for(statement in body)
            tcTop.add( statement.typeCheck(this) )
        return tcTop
    }

    fun typeCheck() : TcTop {
        return typeCheck(this)
    }

}

class TcTop : TcBlock(nullLocation) {

    private val backendFunction = backend.Function("<top>")

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


    override fun codeGen() {
        currentFunction = backendFunction
        currentFunction.add(backend.InstrStart())

        for(stmt in body)
            stmt.codeGen()

        val main = allFunctions.find { it.name == "main" }
        if (main != null)
            currentFunction.add(backend.InstrJsr(main))

        currentFunction.add(backend.InstrEnd())
    }

}

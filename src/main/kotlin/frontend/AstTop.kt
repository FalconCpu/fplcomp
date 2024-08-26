package frontend

import backend.allFunctions

var currentFunction = backend.Function("<dummy>")

class AstTop : AstBlock(nullLocation, null) {

    private val backendFunction = backend.Function("<top>")

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

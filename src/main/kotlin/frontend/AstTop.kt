package frontend

import backend.InstrEnd
import backend.StdlibFatal
import backend.allFunctions
import backend.regArg1

var currentFunction = backend.Function("<dummy>", UnitType)

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

    private val backendFunction = backend.Function("<top>", UnitType)

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

        val main = allFunctions.find { it.name.startsWith("main(") }
        if (main != null)
            currentFunction.add(backend.InstrCall(main))

        currentFunction.add(backend.InstrEnd())
        val nullCheckLabel = currentFunction.failedNullCheckLabel
        if (nullCheckLabel!=null) {
            currentFunction.instrLabel(nullCheckLabel)
            currentFunction.instrMove(regArg1, 4)  // ERROR_NULL_POINTER defined in Fatal.fpl
            currentFunction.instrMove(regArg1, 0)
            currentFunction.instrCall(StdlibFatal)
            currentFunction.add(InstrEnd())
        }

    }

}

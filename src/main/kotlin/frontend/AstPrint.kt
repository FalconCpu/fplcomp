package frontend

import backend.InstrCall

class AstPrint(
    location: Location,
    private val exprs: List<AstExpr>,
    private val newline: Boolean,
    private val hexFormat : Boolean
) : AstStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("PRINT\n")
        for (expr in exprs)
            expr.dump(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) : TcStmt {
        val exprs = exprs.map { it.typeCheck(context) }
        return TcPrint(location, exprs, newline, hexFormat)
    }
}

class TcPrint(
    location: Location,
    private val exprs: List<TcExpr>,
    private val newline: Boolean,
    private val hexFormat: Boolean
) : TcStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("PRINT\n")
        for (expr in exprs)
            expr.dump(sb, indent + 1)
    }

    override fun codeGen() {
        for (expr in exprs) {
            val e = expr.codeGenRvalue()
            currentFunction.instrMove( backend.regArg1, e)
            val func = when(expr.type) {
                CharType -> backend.StdlibPrintChar
                IntType -> if (hexFormat) backend.StdlibPrintHex else backend.StdlibPrintInt
                StringType -> backend.StdlibPrintString
                BoolType -> backend.StdlibPrintBool
                else -> return Log.error(location, "Unsupported type for print ${expr.type}")
            }
            currentFunction.instrCall(func, false)
        }
        if (newline)
            currentFunction.instrCall(backend.StdlibNewline, false)
    }

}

package frontend

import backend.InstrJsr

class AstPrint(
    location: Location,
    private val exprs: List<AstExpr>,
    private val newline: Boolean
) : AstStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("PRINT\n")
        for (expr in exprs)
            expr.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("PRINT\n")
        for (expr in exprs)
            expr.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        for (expr in exprs)
            expr.typeCheck(context)
    }

    override fun codeGen() {
        for (expr in exprs) {
            val e = expr.codeGenRvalue()
            currentFunction.instrMove( backend.regArg1, e)
            val func = when(expr.type) {
                CharType -> backend.StdlibPrintChar
                IntType -> backend.StdlibPrintInt
                StringType -> backend.StdlibPrintString
                BoolType -> backend.StdlibPrintBool
                else -> return Log.error(location, "Unsupported type for print ${expr.type}")
            }
            currentFunction.add(InstrJsr(func))
        }
        if (newline)
            currentFunction.add(InstrJsr(backend.StdlibNewline))
    }



}
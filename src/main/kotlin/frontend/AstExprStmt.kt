package frontend

class AstExprStmt(
    location: Location,
    private val expr: AstExpr
) : AstStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("EXPR\n")
        expr.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("EXPR\n")
        expr.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        expr.typeCheck(context)
        if (expr !is AstFuncCall)
            Log.warning(location, "Expression has no effect")
    }

    override fun codeGen() {
        expr.codeGenRvalue()
    }
}
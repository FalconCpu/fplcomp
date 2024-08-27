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

    override fun typeCheck(context: AstBlock) : TcStmt {
        val expr = expr.typeCheck(context)
        if (expr !is TcFuncCall)
            Log.warning(location, "Expression has no effect")
        return TcExprStmt(location, expr)
    }

}

class TcExprStmt(
    location: Location,
    private val expr: TcExpr
) : TcStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("EXPR\n")
        expr.dump(sb, indent + 1)
    }

    override fun codeGen() {
        expr.codeGenRvalue()
    }

}

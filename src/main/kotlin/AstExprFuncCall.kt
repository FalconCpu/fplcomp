package falcon

class AstExprFuncCall (
    location: Location,
    private val func: AstExpr,
    private val args: List<AstExpr>
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("FUNCCALL\n")
        func.dump(sb, indent + 1)
        for (arg in args) {
            arg.dump(sb, indent + 1)
        }
    }
}
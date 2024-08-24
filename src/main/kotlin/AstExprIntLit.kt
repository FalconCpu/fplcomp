package falcon

class AstExprIntLit(
    location: Location,
    private val value: Int
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("INTLIT $value\n")
    }
}
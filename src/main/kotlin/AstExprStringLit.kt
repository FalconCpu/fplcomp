package falcon

class AstExprStringLit(
    location: Location,
    private val value: String
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("STRINGLIT $value\n")
    }
}
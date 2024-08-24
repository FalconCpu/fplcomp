package falcon

class TcExprStringLit(
    location: Location,
    private val value: String
) : TcExpr(location, StringType) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("STRINGLIT $value\n")
    }
}
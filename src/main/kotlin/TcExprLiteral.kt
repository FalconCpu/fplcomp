package falcon

class TcExprLiteral(
    location: Location,
    type : Type,
    private val value: Value
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("LITERAL $value $type\n")
    }

}
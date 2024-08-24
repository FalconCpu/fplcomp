package falcon

class TcExprError(
    location: Location
) : TcExpr(location, ErrorType) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ERROR\n")
    }

    constructor(location: Location, message: String) : this(location) {
        Log.error(location, message)
    }
}
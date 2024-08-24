package falcon

class TcExprLocalVar(
    location: Location,
    private val symbol: SymbolLocalVar
) : TcExpr(location, symbol.type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("LOCALVAR $symbol $type\n")
    }

}
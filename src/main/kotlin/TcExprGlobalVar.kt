package falcon

class TcExprGlobalVar(
    location: Location,
    private val symbol: SymbolGlobalVar
) : TcExpr(location, symbol.type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("GLOBALVAR $symbol $type\n")
    }

}
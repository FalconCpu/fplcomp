package frontend

class AstParameter(
    location: Location,
    private val kind: TokenKind,
    private val name: String,
    private val type: AstType
) : Ast(location) {

    lateinit var symbol: Symbol

    override fun dump(sb: StringBuilder, indent: Int) {
        val kindTxt = if (kind==TokenKind.EOF) "" else " $kind"
        sb.append(". ".repeat(indent))
        sb.append("PARAMETER $name$kindTxt\n")
        type.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("PARAMETER ${symbol.description()} ${symbol.type}\n")
    }
    fun resolveParameter(context:AstBlock): Symbol {
        val type = type.resolveType(context)
        symbol = when(kind) {
            TokenKind.EOF -> SymbolLocalVar(location, name, type, false)
            TokenKind.VAR -> SymbolField(location, name, type, true)
            TokenKind.VAL -> SymbolField(location, name, type, false)
            else -> error("Invalid kind $kind")
        }
        return symbol
    }

}
package falcon

class AstExprIdentifier (
    location: Location,
    private val name: String
) : AstExpr(location){

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IDENTIFIER $name\n")
    }

    private fun makeNewSymbol(symbolTable: SymbolTable): Symbol {
        Log.error(location,"Undefined identifier: $name")
        val symbol = SymbolLocalVar(location, name, ErrorType, true)
        symbolTable.add(symbol)
        return symbol
    }

    override fun typeCheckRvalue(symbolTable: SymbolTable): TcExpr {
        val symbol = symbolTable.lookup(name) ?: makeNewSymbol(symbolTable)
        return when (symbol) {
            is SymbolLocalVar     -> TcExprLocalVar(location, symbol)
            is SymbolFunctionName -> TODO()
            is SymbolGlobalVar    -> TcExprGlobalVar(location, symbol)
            is SymbolLiteral      -> TODO()
            is SymbolTypeName     -> TODO()
        }
    }

}

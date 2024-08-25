package falcon

class AstIdentifier (
    location: Location,
    private val name: String
) : AstExpr(location){

    lateinit var symbol: Symbol

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IDENTIFIER $name\n")
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IDENTIFIER ${symbol.description()} $type\n")
    }


    private fun makeNewSymbol(symbolTable: AstBlock): Symbol {
        Log.error(location,"Undefined identifier: $name")
        val symbol = SymbolLocalVar(location, name, ErrorType, true)
        symbolTable.add(symbol)
        return symbol
    }

    override fun typeCheckAllowTypeName(context: AstBlock) {
        symbol = predefinedSymbols.lookup(name) ?: context.lookup(name) ?: makeNewSymbol(context)
        type = currentPathContext.smartCasts[symbol] ?: symbol.type
    }

    override fun typeCheck(context:AstBlock) {
        // Symbol has been used as an rvalue
        typeCheckAllowTypeName(context)
        if (symbol is SymbolTypeName)
            Log.error(location, "Got type name '$symbol' when expecting a value")
        if (symbol in currentPathContext.uninitializedVariables)
            Log.error(location, "Variable '$name' has not been initialized")
        else if (symbol in currentPathContext.maybeUninitializedVariables)
            Log.error(location, "Variable '$name' might not be initialized")

    }


    override fun typeCheckLvalue(context: AstBlock) {
        // Symbol has been used as an lvalue
        symbol = predefinedSymbols.lookup(name) ?: context.lookup(name) ?: makeNewSymbol(context)
        type = symbol.type

        when(val sym = symbol) {
            is SymbolField -> TODO()

            is SymbolGlobalVar ->
                if (!sym.isMutable)
                    Log.error(location, "Global variable $name is not mutable")

            is SymbolLocalVar ->
                if (!sym.isMutable && sym !in currentPathContext.uninitializedVariables)
                    Log.error(location, "Local variable $name is not mutable")

            is SymbolFunctionName,
            is SymbolLiteral,
            is SymbolTypeName -> Log.error(location, "Not an lvalue: $name")
        }

        currentPathContext = currentPathContext.initializeVariable(symbol)
    }

}

package falcon

class SymbolTable(
    private val parent: SymbolTable?,
) {
    private val symbols = mutableMapOf<String, Symbol>()

    fun add(symbol: Symbol) {
        val duplicate = symbols[symbol.name]
        if (duplicate != null)
            Log.error(symbol.location, "duplicate symbol: $symbol")
        symbols[symbol.name] = symbol
    }

    fun lookup(name: String): Symbol? = symbols[name] ?: parent?.lookup(name)

    fun lookupNoHierarchy(name: String): Symbol? = symbols[name]

}
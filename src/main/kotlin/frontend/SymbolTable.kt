package frontend

class SymbolTable (
    private val parent : SymbolTable?
) {
    private val symbols = mutableMapOf<String, Symbol>()

    fun add(symbol: Symbol) {
        val duplicate = symbols[symbol.name]
        if (duplicate != null)
            Log.error(symbol.location, "duplicate symbol: $symbol")
        symbols[symbol.name] = symbol
    }

    fun replace(symbol: Symbol) {
        check(symbols.contains(symbol.name))
        symbols[symbol.name] = symbol
    }

    fun lookup(name: String): Symbol? = symbols[name] ?: parent?.lookup(name)

    fun lookupNoHierarchy(name: String): Symbol? = symbols[name]

    fun clear() {
        symbols.clear()
    }

    fun removeLocals() {
        val locals = symbols.values.filterIsInstance<SymbolLocalVar>()
        locals.forEach { symbols.remove(it.name) }
    }

    fun import(other: SymbolTable) {
        for(sym in other.symbols.values)
            add(sym)
    }

    fun getMethods() = symbols.values.filterIsInstance<SymbolFunctionName>()
}
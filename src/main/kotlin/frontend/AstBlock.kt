package frontend

sealed class AstBlock(location: Location, val parent:AstBlock?) : AstStmt(location) {
    private val symbolTable = mutableMapOf<String, Symbol>()
    protected val body = mutableListOf<AstStmt>()

    // --------------------------------------------------------
    //                 Symbol table functions
    // --------------------------------------------------------

    fun add(symbol: Symbol) {
        val duplicate = symbolTable[symbol.name]
        if (duplicate != null)
            Log.error(symbol.location, "duplicate symbol: $symbol")
        symbolTable[symbol.name] = symbol
    }

    fun lookup(name: String): Symbol? = symbolTable[name] ?: parent?.lookup(name)

    fun lookupNoHierarchy(name: String): Symbol? = symbolTable[name]

    fun replace(symbol: Symbol) {
        check(symbolTable.contains(symbol.name))
        symbolTable[symbol.name] = symbol
    }

    fun import(other:AstBlock) {
        symbolTable.clear()
        for (sym in other.symbolTable.values)
            add(sym)
    }

    fun removeLocals() {
        val locals = symbolTable.values.filterIsInstance<SymbolLocalVar>()
        locals.forEach { symbolTable.remove(it.name) }
    }

    fun getMethods() = symbolTable.values.filterIsInstance<SymbolFunctionName>()

    // --------------------------------------------------------
    //                 Statement list functions
    // --------------------------------------------------------

    fun add(stmt: AstStmt) {
        body.add(stmt)
    }

    open fun identifyFunctions(context: AstBlock) {
        for (stmt in body)
            if (stmt is AstBlock)
                stmt.identifyFunctions(this)
    }
}

sealed class TcBlock(
    location: Location
) : TcStmt(location) {
    val body = mutableListOf<TcStmt>()

    fun add(stmt: TcStmt) {
        body.add(stmt)
    }

}
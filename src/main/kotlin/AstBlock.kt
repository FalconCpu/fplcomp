package falcon

sealed class AstBlock(location: Location, protected val parent:AstBlock?) : AstStmt(location) {
    protected val symbolTable = mutableMapOf<String, Symbol>()
    protected val body = mutableListOf<AstStmt>()


    fun add(symbol: Symbol) {
        val duplicate = this.symbolTable[symbol.name]
        if (duplicate != null)
            Log.error(symbol.location, "duplicate symbol: $symbol")
        this.symbolTable[symbol.name] = symbol
    }

    fun replace(symbol: Symbol) {
        check(this.symbolTable[symbol.name] != null)
        this.symbolTable[symbol.name] = symbol
    }

    open fun lookup(name: String): Symbol? = this.symbolTable[name] ?: parent?.lookup(name)

    fun lookupNoHierarchy(name: String): Symbol? = this.symbolTable[name]

    fun add(stmt: AstStmt) {
        body.add(stmt)
    }

    open fun identifyFunctions(context: AstBlock) {
        for (stmt in body)
            if (stmt is AstBlock)
                stmt.identifyFunctions(this)
    }

    fun findEnclosingFunction(): AstFunction? {
        var current : AstBlock? = this
        while (current !=null) {
            if (current is AstFunction)
                return current
            current = current.parent
        }
        return null
    }
}
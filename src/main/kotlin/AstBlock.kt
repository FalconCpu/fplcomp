package falcon

sealed class AstBlock(location: Location, private val parent:AstBlock?) : AstStmt(location) {
    private val symbolTable = mutableMapOf<String, Symbol>()
    protected val body = mutableListOf<AstStmt>()


    fun add(symbol: Symbol) {
        val duplicate = this.symbolTable[symbol.name]
        if (duplicate != null)
            Log.error(symbol.location, "duplicate symbol: $symbol")
        this.symbolTable[symbol.name] = symbol
    }

    fun lookup(name: String): Symbol? = this.symbolTable[name] ?: parent?.lookup(name)

    fun lookupNoHierarchy(name: String): Symbol? = this.symbolTable[name]

    fun add(stmt: AstStmt) {
        body.add(stmt)
    }

    open fun identifyFunctions(context: AstBlock) {
        for (stmt in body)
            if (stmt is AstBlock)
                stmt.identifyFunctions(this)
    }

    fun findEnclosingFunction(): AstBlockFunction? {
        var current : AstBlock? = this
        while (current !=null) {
            if (current is AstBlockFunction)
                return current
            current = current.parent
        }
        return null
    }
}
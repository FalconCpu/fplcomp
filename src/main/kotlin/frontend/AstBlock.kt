package frontend

sealed class AstBlock(location: Location, parent:AstBlock?) : AstStmt(location) {
    protected val symbolTable : SymbolTable = SymbolTable(parent?.symbolTable)
    protected val body = mutableListOf<AstStmt>()

    fun add(stmt: AstStmt) {
        body.add(stmt)
    }

    open fun identifyFunctions(context: AstBlock) {
        for (stmt in body)
            if (stmt is AstBlock)
                stmt.identifyFunctions(this)
    }

    fun add(symbol: Symbol) {
        symbolTable.add(symbol)
    }

    fun lookup(name: String): Symbol? = symbolTable.lookup(name)

    fun lookupNoHierarchy(name: String): Symbol? = symbolTable.lookupNoHierarchy(name)

    fun replace(symbol: Symbol) {
        symbolTable.replace(symbol)
    }
}

sealed class TcBlock(
    location: Location,
    val symbolTable: SymbolTable?,
) : TcStmt(location) {
    val body = mutableListOf<TcStmt>()

    fun add(stmt: TcStmt) {
        body.add(stmt)
    }

}
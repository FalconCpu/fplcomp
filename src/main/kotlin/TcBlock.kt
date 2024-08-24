package falcon

sealed class TcBlock(
    location: Location,
    val symbolTable: SymbolTable
) : TcStmt(location) {
    protected val body = mutableListOf<TcStmt>()

    fun add(stmt: TcStmt) {
        body.add(stmt)
    }
}
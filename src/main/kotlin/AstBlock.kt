package falcon

sealed class AstBlock(location: Location, private val parent:AstBlock?) : AstStmt(location) {
    protected val body = mutableListOf<AstStmt>()
    val symbolTable : SymbolTable = SymbolTable(parent?.symbolTable)

    fun add(stmt: AstStmt) {
        body.add(stmt)
    }

}
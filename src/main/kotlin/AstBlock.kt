package falcon

sealed class AstBlock(location: Location) : AstStmt(location) {
    protected val body = mutableListOf<AstStmt>()

    fun add(stmt: AstStmt) {
        body.add(stmt)
    }
}
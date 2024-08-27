package frontend

sealed class AstStmt(location: Location) : Ast(location) {
    abstract fun typeCheck(context:AstBlock) : TcStmt
}

sealed class TcStmt(location: Location) : Tc(location) {
    abstract fun codeGen()
}

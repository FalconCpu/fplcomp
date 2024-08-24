package falcon

sealed class AstStmt(location: Location) : Ast(location) {

    abstract fun typeCheck(context:TcBlock)

}

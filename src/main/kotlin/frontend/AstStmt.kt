package frontend

sealed class AstStmt(location: Location) : Ast(location) {

    abstract fun typeCheck(context:AstBlock)

}

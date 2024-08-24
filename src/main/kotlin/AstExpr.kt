package falcon

sealed class AstExpr(location: Location) : Ast(location) {

    abstract fun typeCheckRvalue(context:SymbolTable): TcExpr
}
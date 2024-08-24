package falcon

sealed class AstType(location: Location) : Ast(location) {

    abstract fun resolveType(context:TcBlock) : Type

}

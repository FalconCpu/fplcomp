package frontend

sealed class AstType(location: Location) : Ast(location) {

    abstract fun resolveType(context:AstBlock) : Type

}

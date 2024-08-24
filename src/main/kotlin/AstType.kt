package falcon

sealed class AstType(location: Location) : Ast(location) {

    abstract fun resolveType(context:AstBlock) : Type

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        error("Should not be called - types should have been resolved")
    }
}

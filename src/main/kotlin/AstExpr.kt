package falcon

sealed class AstExpr(location: Location) : Ast(location) {
    lateinit var type: Type

    abstract fun typeCheck(context:AstBlock)

    open fun typeCheckLvalue(context: AstBlock) {
        Log.error(location, "Not an lvalue")
        type = ErrorType
    }

    open fun isMutable(): Boolean = false

    fun setTypeError(message:String) {
        Log.error(location, message)
        type = ErrorType
    }

    fun setTypeError() {
        type = ErrorType
    }

    open fun typeCheckAllowTypeName(context: AstBlock) {
        typeCheck(context)
    }

    /**
     * Return a symbol that represents this expression for smart casts.
     * If there is no symbol, return null.
     */
    fun getSmartCastSymbol() =
        when (this) {
            is AstIdentifier -> symbol
            is AstMemberAccess -> smartCastSymbol
            else -> null
        }
}
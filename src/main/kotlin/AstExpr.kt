package falcon

sealed class AstExpr(location: Location) : Ast(location) {
    lateinit var type: Type

    abstract fun typeCheck(context:AstBlock)

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

    open fun checkIsLvalue() {
        Log.error(location, "Not an lvalue")
    }

}
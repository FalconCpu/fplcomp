package frontend

import backend.Label
import backend.Reg
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class AstExpr(location: Location) : Ast(location) {
    lateinit var type: Type

    abstract fun typeCheck(context:AstBlock)

    open fun typeCheckLvalue(context: AstBlock) {
        Log.error(location, "Not an lvalue")
        type = ErrorType
    }

    abstract fun codeGenRvalue() : Reg

    open fun codeGenLvalue(reg: Reg) {
        error("codeGenLvalue called on non lvalue")
    }

    open fun codeGenBool(trueLabel: Label, falseLabel:Label) {
        TODO("Not implemented codeGenBool ${this.javaClass}")
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

    @OptIn(ExperimentalContracts::class)
    fun isTypeName() : Boolean {
        contract {
            returns(true) implies (this@AstExpr is AstIdentifier)
        }
        return this is AstIdentifier && this.symbol is SymbolTypeName
    }

    fun isFunctionName() : SymbolFunctionName? {
        if (this !is AstIdentifier)
            return null
        val symbol = this.symbol
        if (symbol !is SymbolFunctionName)
            return null
        return symbol
    }

    override fun dump(sb: StringBuilder, indent: Int) {
        TODO("Not yet implemented")
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        TODO("Not yet implemented")
    }
}
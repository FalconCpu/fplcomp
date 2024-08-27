package frontend

import backend.Label
import backend.Reg
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class AstExpr(location: Location) : Ast(location) {

    abstract fun typeCheck(context:AstBlock) : TcExpr

    open fun typeCheckLvalue(context: AstBlock) : TcExpr {
        return TcError(location, "Not an lvalue")
    }

    open fun typeCheckAllowTypeName(context: AstBlock) : TcExpr {
        return typeCheck(context)
    }

}

sealed class TcExpr(location: Location, val type: Type) : Tc(location){

    abstract fun codeGenRvalue() : Reg

    open fun codeGenLvalue(reg: Reg) {
        error("codeGenLvalue called on non lvalue")
    }

    open fun codeGenBool(trueLabel: Label, falseLabel:Label) {
        TODO("Not implemented codeGenBool ${this.javaClass}")
    }

    fun getSmartCastSymbol() =
        when (this) {
            is TcIdentifier -> symbol
            is TcMemberAccess -> smartCastSymbol
            else -> null
        }

    open fun isMutable(): Boolean = false

    @OptIn(ExperimentalContracts::class)
    fun isTypeName() : Boolean {
        contract {
            returns(true) implies (this@TcExpr is TcIdentifier)
        }
        return this is TcIdentifier && this.symbol is SymbolTypeName
    }

    fun isFunctionName() : SymbolFunctionName? {
        if (this !is TcIdentifier)
            return null
        val symbol = this.symbol
        if (symbol !is SymbolFunctionName)
            return null
        return symbol
    }
}
package frontend

import backend.AluOp
import backend.Label
import backend.Reg
import backend.regZero
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class AstExpr(location: Location) : Ast(location) {

    abstract fun typeCheck(context:AstBlock) : TcExpr

    open fun typeCheckLvalue(context: AstBlock) : TcExpr {
        return TcError(location, "Not an lvalue")
    }

    open fun typeCheckAllowType(context:AstBlock) : TcExpr {
        return typeCheck(context)
    }

//    open fun evaluateAsTypeName(context: AstBlock) : Type? {
//        return null
//    }
//
//    open fun evaluateAsFunctionName(context: AstBlock) : List<TcFunction>? {
//        return null
//    }

}

sealed class TcExpr(location: Location, val type: Type) : Tc(location){

    abstract fun codeGenRvalue() : Reg

    open fun codeGenLvalue(reg: Reg) {
        error("codeGenLvalue called on non lvalue")
    }

    open fun codeGenBool(trueLabel: Label, falseLabel:Label) {
        val tmp = codeGenRvalue()
        currentFunction.instrBranch(AluOp.NE_I, tmp, regZero, trueLabel)
        currentFunction.instrJump(falseLabel)
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
}
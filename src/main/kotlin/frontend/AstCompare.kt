package frontend

import backend.AluOp
import backend.Label
import backend.Reg
import backend.StdlibStrcmp
import backend.regArg1
import backend.regArg2
import backend.regZero

class AstCompare(
    location: Location,
    private val op: TokenKind,
    private val lhs: AstExpr,
    private val rhs: AstExpr
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("COMPARE $op\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) : TcExpr {
        trueBranchContext = currentPathContext
        falseBranchContext = currentPathContext

        val lhs = lhs.typeCheck(context)
        val rhs = rhs.typeCheck(context)
        if (lhs.type == ErrorType) return lhs
        if (rhs.type == ErrorType) return rhs

        if (!( (lhs.type == IntType  && rhs.type == IntType) ||
               (lhs.type == CharType && rhs.type == CharType) ||
               (lhs.type == RealType && rhs.type == RealType) ||
               (lhs.type == StringType && rhs.type == StringType) ) )
            return TcError(location,"No operation defined for ${lhs.type} $op ${rhs.type}")

        val op = when (op) {
            TokenKind.LT -> AluOp.LT_I
            TokenKind.LTE -> AluOp.LE_I
            TokenKind.GT -> AluOp.GT_I
            TokenKind.GTE -> AluOp.GE_I
            else -> error("Unknown operator")
        }

        return TcCompare(location, op, lhs, rhs)
    }
}

class TcCompare(
    location: Location,
    private val op: AluOp,
    private val lhs: TcExpr,
    private val rhs: TcExpr
) : TcExpr(location, BoolType) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("COMPARE $op $type\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
    }

    override fun codeGenRvalue(): Reg {
        val lhsReg = lhs.codeGenRvalue()
        val rhsReg = rhs.codeGenRvalue()

        return when (lhs.type) {
            is IntType -> currentFunction.instrAlu(op, lhsReg, rhsReg)

            is StringType -> {
                currentFunction.instrMove(regArg1, lhsReg)
                currentFunction.instrMove(regArg2, rhsReg)
                val tmp = currentFunction.instrCall(StdlibStrcmp)
                currentFunction.instrAlu(op, regZero, tmp)
            }

            is RealType -> TODO("Real comparison not yet implemented")

            else -> error("Invalid type to compare")
        }
    }

    fun codeGenStringBool(trueLabel: Label, falseLabel: Label) {
        val lhs = lhs.codeGenRvalue()
        val rhs = rhs.codeGenRvalue()
        currentFunction.instrMove(regArg1, lhs)
        currentFunction.instrMove(regArg2, rhs)
        val tmp = currentFunction.instrCall(StdlibStrcmp)
        currentFunction.instrBranch(op, tmp, regZero, trueLabel)
        currentFunction.instrJump(falseLabel)
    }

    fun codeGenIntBool(trueLabel: Label, falseLabel: Label) {
        check( (lhs.type==IntType && rhs.type == IntType)
            || (lhs.type == CharType && rhs.type == CharType) )
        {"TODO: String and Real compare"}

        val lhs = lhs.codeGenRvalue()
        val rhs = rhs.codeGenRvalue()
        currentFunction.instrBranch(op, lhs, rhs, trueLabel)
        currentFunction.instrJump(falseLabel)
    }

    override fun codeGenBool(trueLabel: Label, falseLabel: Label) {
        when(lhs.type) {
            IntType -> codeGenIntBool(trueLabel, falseLabel)
            CharType -> codeGenIntBool(trueLabel, falseLabel)
            RealType -> TODO("Real comparison not implemented")
            StringType -> codeGenStringBool(trueLabel, falseLabel)
            else -> error("Unknown type")
        }
    }
}

package frontend

import backend.AluOp
import backend.Label
import backend.Reg

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
        TODO("Not yet implemented")
    }

    override fun codeGenBool(trueLabel: Label, falseLabel: Label) {
        check(lhs.type==IntType && rhs.type == IntType) {"TODO: String and Real compare"}

        val lhs = lhs.codeGenRvalue()
        val rhs = rhs.codeGenRvalue()
        currentFunction.instrBranch(op, lhs, rhs, trueLabel)
        currentFunction.instrJump(falseLabel)
    }

}

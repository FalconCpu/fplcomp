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

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("COMPARE $op $type\n")
        lhs.dumpWithType(sb, indent + 1)
        rhs.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        require(op in listOf(TokenKind.LT, TokenKind.LTE, TokenKind.GT, TokenKind.GTE))
        trueBranchContext = currentPathContext
        falseBranchContext = currentPathContext

        lhs.typeCheck(context)
        rhs.typeCheck(context)

        if ((lhs.type == IntType && rhs.type == IntType) ||
            (lhs.type == RealType && rhs.type == RealType) ||
            (lhs.type == StringType && rhs.type == StringType) ) {
            // No smart casts
        } else
            Log.error(location, "No operation defined for ${lhs.type} $op ${rhs.type}")

        type = BoolType
    }

    override fun codeGenRvalue(): Reg {
        TODO("Not yet implemented")
    }

    override fun codeGenBool(trueLabel: Label, falseLabel: Label) {
        val lhs = lhs.codeGenRvalue()
        val rhs = rhs.codeGenRvalue()
        val op = when (op) {
            TokenKind.LT -> AluOp.LT_I
            TokenKind.LTE -> AluOp.LE_I
            TokenKind.GT -> AluOp.GT_I
            TokenKind.GTE -> AluOp.GE_I
            else -> error("Unknown operator")
        }
        currentFunction.instrBranch(op, lhs, rhs, trueLabel)
        currentFunction.instrJump(falseLabel)
    }
}
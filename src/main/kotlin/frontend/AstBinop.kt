package frontend

import backend.AluOp
import backend.Reg

class AstBinop(
    location: Location,
    private val op: TokenKind,
    private val lhs: AstExpr,
    private val rhs: AstExpr
) : AstExpr(location) {
    private lateinit var opType: AluOp

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("BINARYOP $op\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("BINARYOP $op $type\n")
        lhs.dumpWithType(sb, indent + 1)
        rhs.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        lhs.typeCheck(context)
        rhs.typeCheck(context)
        if (lhs.type==ErrorType || rhs.type == ErrorType)
            return setTypeError()
        val match = operatorTable.find { it.kind == op && it.lhsType == lhs.type && it.rhsType == rhs.type }
        if (match == null)
            return setTypeError("No operation defined for ${lhs.type} $op ${rhs.type}")
        type = match.resultType
        opType = match.op
    }

    override fun codeGenRvalue(): Reg {
        val lhs = lhs.codeGenRvalue()
        val rhs = rhs.codeGenRvalue()
        return currentFunction.instrAlu(opType, lhs, rhs)
    }

}

private class Operator(val kind: TokenKind, val lhsType:Type, val rhsType: Type, val op: AluOp, val resultType:Type)
private val operatorTable = listOf(
    Operator(TokenKind.PLUS,    IntType, IntType, AluOp.ADD_I, IntType),
    Operator(TokenKind.MINUS,   IntType, IntType, AluOp.SUB_I, IntType),
    Operator(TokenKind.STAR,    IntType, IntType, AluOp.MUL_I, IntType),
    Operator(TokenKind.SLASH,   IntType, IntType, AluOp.DIV_I, IntType),
    Operator(TokenKind.PERCENT, IntType, IntType, AluOp.MOD_I, IntType),
    Operator(TokenKind.AMPERSAND,IntType,IntType, AluOp.AND_I, IntType),
    Operator(TokenKind.BAR,     IntType, IntType, AluOp.OR_I, IntType),
    Operator(TokenKind.CARET,   IntType, IntType, AluOp.XOR_I, IntType),
//    Operator(TokenKind.LEFT,    IntType, IntType, AluOp.SHL_I, IntType),
//    Operator(TokenKind.RIGHT,   IntType, IntType, AluOp.SHR_I, IntType),
)

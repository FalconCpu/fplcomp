package frontend

import backend.*

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

    override fun typeCheck(context: AstBlock) : TcExpr{
        val lhs = lhs.typeCheck(context)
        val rhs = rhs.typeCheck(context)
        if (lhs.type==ErrorType) return lhs
        if (rhs.type==ErrorType) return rhs

        if (lhs.type==StringType && rhs.type==StringType && op==TokenKind.PLUS)
            return TcStringCat(location, lhs, rhs)

        val match = operatorTable.find { it.kind == op && it.lhsType == lhs.type && it.rhsType == rhs.type }
        if (match == null)
            return TcError(location, "No operation defined for ${lhs.type} $op ${rhs.type}")
        return TcBinop(location, match.resultType, match.op, lhs, rhs)
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
    Operator(TokenKind.SHL,     IntType, IntType, AluOp.LSL_I, IntType),
    Operator(TokenKind.SHR,     IntType, IntType, AluOp.ASR_I, IntType),
    Operator(TokenKind.USHR,    IntType, IntType, AluOp.LSR_I, IntType)
)

class TcBinop(
    location: Location,
    type: Type,
    val op : AluOp,
    val lhs: TcExpr,
    val rhs: TcExpr
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("BINARYOP $op $type\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
    }

    override fun codeGenRvalue(): Reg {
        val lhs = lhs.codeGenRvalue()
        val rhs = rhs.codeGenRvalue()
        return currentFunction.instrAlu(op, lhs, rhs)
    }
}


class TcStringCat(
    location: Location,
    val lhs: TcExpr,
    val rhs: TcExpr
) : TcExpr(location, StringType) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("STRCAT $type\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
    }

    override fun codeGenRvalue(): Reg {
        val lhs = lhs.codeGenRvalue()
        val rhs = rhs.codeGenRvalue()
        currentFunction.instrMove(regArg1, lhs)
        currentFunction.instrMove(regArg2, rhs)
        return currentFunction.instrCall(StdlibStrcat)
    }

}



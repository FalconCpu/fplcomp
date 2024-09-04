package frontend

import backend.AluOp
import backend.IntValue
import backend.Reg
import backend.regZero

class AstNeg(
    location: Location,
    private val expr: AstExpr
) : AstExpr(location) {
    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("NEG\n")
        expr.dump(sb, indent + 1)
    }


    override fun typeCheck(context: AstBlock) : TcExpr {
        val expr = expr.typeCheck(context)
        return when (expr.type) {
            IntType ->
                if (expr is TcIntLiteral)
                    TcIntLiteral(expr.location, IntType, -expr.value)
                else
                    TcNeg(location, IntType, expr)
            RealType -> TcNeg(location, RealType, expr)
            else -> TcError(location, "No operation defined for unary minus ${expr.type}")
        }
    }

}

class TcNeg(
    location: Location,
    type : Type,
    private val expr: TcExpr
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("NEG $type\n")
        expr.dump(sb, indent + 1)
    }

    override fun codeGenRvalue(): Reg {
        return when(type) {
            is IntType -> currentFunction.instrAlu(AluOp.SUB_I, regZero, expr.codeGenRvalue())
            is RealType -> TODO("Real Numbers")
            else -> error("Unknown type ${type}")
        }
    }
}

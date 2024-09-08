package frontend

import backend.AluOp
import backend.Reg
import backend.regZero

class AstNullCheck(
    location: Location,
    private val astExpr: AstExpr
) : AstExpr(location) {
    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("NULLCHECK\n")
        astExpr.dump(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock): TcExpr {
        val expr = astExpr.typeCheck(context)
        if (expr.type is ErrorType)
            return expr
        if (expr.type is NullableType) {
            val smartCastSymbol = expr.getSmartCastSymbol()
            currentPathContext = currentPathContext.addSmartCast(smartCastSymbol, expr.type.base)
            return TcNullCheck(location, expr.type.base, expr)
        }
        return TcError(location, "Expected nullable type")
    }
}


class TcNullCheck(
    location: Location,
    type: Type,
    private val expr: TcExpr
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("NULLCHECK $type\n")
        expr.dump(sb, indent + 1)
    }

    override fun codeGenRvalue(): Reg {
        val expr = expr.codeGenRvalue()

        val label = currentFunction.failedNullCheckLabel ?: currentFunction.newLabel()
        currentFunction.failedNullCheckLabel = label
        currentFunction.instrBranch(AluOp.EQ_I, expr, regZero, label)
        return expr
    }
}
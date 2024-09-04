package frontend
import backend.Reg
import backend.AluOp

class AstBitwiseInvert (
    location: Location,
    private val expr: AstExpr
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("INVERT\n")
        expr.dump(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock): TcExpr {
        val expr = expr.typeCheck(context)
        IntType.checkAssignCompatible(location, expr.type)

        return TcBitwiseInvert(location, expr)
    }
}

class TcBitwiseInvert(
    location: Location,
    private val expr: TcExpr
) : TcExpr(location, IntType) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("INVERT $type\n")
        expr.dump(sb, indent + 1)
    }

    override fun codeGenRvalue(): Reg {
        val v = expr.codeGenRvalue()
        return currentFunction.instrAlu(AluOp.XOR_I, v, -1)
    }
}

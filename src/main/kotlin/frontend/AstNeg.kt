package frontend

import backend.Reg

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
            IntType -> TcNeg(location, IntType, expr)
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
        TODO("Not yet implemented")
    }
}

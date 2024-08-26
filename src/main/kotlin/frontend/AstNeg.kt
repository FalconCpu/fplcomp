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

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("NEG $type\n")
        expr.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        expr.typeCheck(context)
        type = when (expr.type) {
            IntType -> IntType
            RealType -> RealType
            else -> makeTypeError(location, "No operation defined for unary minus ${expr.type}")
        }
    }

    override fun codeGenRvalue(): Reg {
        TODO("Not yet implemented")
    }
}
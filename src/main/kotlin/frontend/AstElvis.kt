package frontend

import backend.Reg

class AstElvis(
    location: Location,
    private val lhs: AstExpr,
    private val rhs: AstExpr
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ELVIS\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
    }


    override fun typeCheck(context: AstBlock) : TcExpr {
        val lhs = lhs.typeCheck(context)
        val rhs = rhs.typeCheck(context)
        if (lhs.type is ErrorType) return lhs
        if (rhs.type is ErrorType) return rhs

        if (lhs.type !is NullableType)
            return TcError(lhs.location, "Not a nullable type for elvis operator: ${lhs.type}")

        if (!lhs.type.isAssignableFrom(rhs.type))
            return TcError(rhs.location, "Incompatible types for elvis operator ${lhs.type} and ${rhs.type}")

        return TcElvis(location, rhs.type, lhs, rhs)
    }

}

class TcElvis(
    location: Location,
    type: Type,
    private val lhs: TcExpr,
    private val rhs: TcExpr
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ELVIS $type\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
    }

    override fun codeGenRvalue(): Reg {
        TODO("Not yet implemented")
    }

}

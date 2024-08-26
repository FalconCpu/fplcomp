package frontend

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

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ELVIS $type\n")
        lhs.dumpWithType(sb, indent + 1)
        rhs.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        lhs.typeCheck(context)
        rhs.typeCheck(context)

        if (lhs.type is ErrorType || rhs.type is ErrorType)
            return setTypeError()

        if (lhs.type !is NullableType)
            return setTypeError("Not a nullable type for elvis operator: ${lhs.type}")

        if (!lhs.type.isAssignCompatible(rhs.type))
            return setTypeError("Incompatible types for elvis operator ${lhs.type} and ${rhs.type}")

        type = rhs.type
    }

}
package frontend

class AstArrayAccess(
    location: Location,
    private val lhs: AstExpr,
    private val index: AstExpr
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYACCESS\n")
        lhs.dump(sb, indent + 1)
        index.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYACCESS $type\n")
        lhs.dumpWithType(sb, indent + 1)
        index.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        lhs.typeCheck(context)
        index.typeCheck(context)
        val lhsType = lhs.type

        if (lhsType is ErrorType || index.type is ErrorType)
            return setTypeError()
        if (lhsType !is ArrayType)
            return setTypeError("Cannot index into type '$lhsType'")

        IntType.checkAssignCompatible(index.location, index.type)
        type = lhsType.elementType
    }
}
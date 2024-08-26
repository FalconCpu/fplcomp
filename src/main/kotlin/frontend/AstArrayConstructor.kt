package frontend

class AstArrayConstructor (
    location: Location,
    private val astElementType: AstType,
    private val size: AstExpr
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYCONSTRUCTOR\n")
        astElementType.dump(sb, indent + 1)
        size.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYCONSTRUCTOR $type\n")
        size.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        val elementType = astElementType.resolveType(context)
        size.typeCheck(context)
        IntType.checkAssignCompatible (location, size.type)
        type = makeArrayType(elementType)
    }

}

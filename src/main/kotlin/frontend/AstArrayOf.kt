package frontend

class AstArrayOf(
    location: Location,
    private val astElementType: AstType?,
    private val elements : List<AstExpr>
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYOF\n")
        astElementType?.dump(sb, indent + 1)
        for (element in elements)
            element.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYOF $type\n")
        for (element in elements)
            element.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        elements.forEach { it.typeCheck(context) }

        if (elements.isEmpty() && astElementType == null)
            return setTypeError("Array of unknown type")

        val elementType = astElementType?.resolveType(context) ?: elements[0].type

        for (element in elements)
            elementType.checkAssignCompatible (element.location, element.type)

        type = makeArrayType(elementType)
    }

}
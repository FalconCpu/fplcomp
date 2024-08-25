package falcon

class AstTypeNullable (
    location: Location,
    private val type: AstType
) : AstType(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("TYPENULLABLE\n")
        type.dump(sb, indent + 1)
    }

    override fun resolveType(context: AstBlock): Type {
        val elementType = type.resolveType(context)
        if (elementType is IntType || elementType is RealType || elementType is BoolType || elementType is CharType)
            return makeTypeError(location, "Cannot make nullable type of primitive type $elementType")

        return makeNullableType(elementType)
    }
}
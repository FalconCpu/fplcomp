package falcon

class AstTypeArray (
    location: Location,
    private val type: AstType
) : AstType(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("TYPEARRAY\n")
        type.dump(sb, indent + 1)
    }

    override fun resolveType(context: AstBlock): Type {
        val elementType = type.resolveType(context)
        return makeArrayType(elementType)
    }
}

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
        TODO("Not yet implemented")
    }
}
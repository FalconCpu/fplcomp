package frontend

class AstTypePointer (
    location: Location,
    private val astType: AstType?
) : AstType(location) {

    override fun resolveType(context: AstBlock): Type {
        return makePointerType(astType?.resolveType(context))
    }

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("POINTER\n")
        astType?.dump(sb, indent + 1)
    }
}
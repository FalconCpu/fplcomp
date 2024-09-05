package frontend

class AstTypeTuple(
    location: Location,
    val types: List<AstType>
) : AstType(location) {
    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("TUPLE\n")
        for (type in types)
            type.dump(sb, indent + 1)
    }

    override fun resolveType(context: AstBlock): Type {
        val types = types.map { it.resolveType(context) }
        return makeTupleType(types)
    }
}

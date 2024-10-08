package frontend

class AstTypeId(
    location: Location,
    private val name: String
) : AstType(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("TYPEID $name\n")
    }

    override fun resolveType(context: AstBlock): Type {
        val symbol = predefinedSymbols.lookup(name) ?: context.lookup(name)
        if (symbol==null)
            return makeTypeError(location,"Undefined identifier: $name")
        if (symbol is SymbolTypeName)
            return symbol.type
        return makeTypeError(location, "$name is not a type")
    }
}
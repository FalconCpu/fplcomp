package falcon

class AstBlockClass(
    location: Location,
    parent: AstBlock,
    private val name: String,
    private val parameters: List<AstParameter>
) : AstBlock(location, parent) {

    private val type = ClassType(name)
    private val symbol = SymbolTypeName(location, name, type)

    init {
        parent.symbolTable.add(symbol)
    }

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CLASS $name\n")
        for (parameter in parameters)
            parameter.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun typeCheck(context: TcBlock) {
        TODO("Not yet implemented")
    }
}
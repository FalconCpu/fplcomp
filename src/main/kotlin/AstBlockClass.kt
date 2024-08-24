package falcon

class AstBlockClass(
    location: Location,
    parent: AstBlock,
    private val name: String,
    private val parameters: List<AstParameter>
) : AstBlock(location, parent) {

    lateinit var params : List<Symbol>
    private val type = makeClassType(name, this)
    private val symbol = SymbolTypeName(location, name, type)


    init {
        parent.add(symbol)
    }

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CLASS $name\n")
        for (parameter in parameters)
            parameter.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CLASS $name\n")
        for (parameter in parameters)
            parameter.dumpWithType(sb, indent + 1)
        for (stmt in body)
            stmt.dumpWithType(sb, indent + 1)
    }

    override fun identifyFunctions(context: AstBlock) {
        params = parameters.map { it.resolveParameter(context) }
        params.forEach { add(it) }
    }

    override fun typeCheck(context: AstBlock) {
        for(statement in body)
            statement.typeCheck(this)
    }


}
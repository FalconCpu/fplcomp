package falcon

class AstBlockClass(
    location: Location,
    private val name: String,
    private val parameters: List<AstParameter>
) : AstBlock(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CLASS $name\n")
        for (parameter in parameters)
            parameter.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

}
package falcon

class AstBlockFunction (
    location: Location,
    parent : AstBlock,
    private val name: String,
    private val args: List<AstParameter>,
    private val returnType: AstType?
) : AstBlock(location, parent) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("FUNCTION $name\n")
        for (arg in args)
            arg.dump(sb, indent + 1)
        returnType?.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun typeCheck(context: TcBlock) {
        TODO("Not yet implemented")
    }

}
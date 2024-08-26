package frontend

class AstSuperClass(
    location: Location,
    val name: String,
    val args: List<AstExpr>,
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("SUPERCLASS $name\n")
        for (arg in args)
            arg.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("SUPERCLASS $name\n")
        for (arg in args)
            arg.dumpWithType(sb, indent + 1)
    }
}
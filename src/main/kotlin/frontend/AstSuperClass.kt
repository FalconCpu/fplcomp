package frontend

// This class represents the call to a superclass constructor

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

    fun resolveArgs(context:AstBlock) : List<TcExpr> {
        return args.map{ it.typeCheck(context) }
    }


}
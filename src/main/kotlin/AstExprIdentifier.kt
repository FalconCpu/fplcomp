package falcon

class AstExprIdentifier (
    location: Location,
    private val name: String
) : AstExpr(location){

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IDENTIFIER $name\n")
    }

}

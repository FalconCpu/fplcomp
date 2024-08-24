package falcon

class AstExprMemberAccess (
    location: Location,
    private val lhs: AstExpr,
    private val name: AstExprIdentifier
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("MEMBERACCESS\n")
        lhs.dump(sb, indent + 1)
        name.dump(sb, indent + 1)
    }

}

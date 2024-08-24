package falcon

class AstStmtAssign(
    location: Location,
    private val lhs: AstExpr,
    private val rhs: AstExpr
) : AstStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ASSIGN\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ASSIGN\n")
        lhs.dumpWithType(sb, indent + 1)
        rhs.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context:AstBlock) {
        lhs.typeCheckLvalue(context)
        rhs.typeCheck(context)
        lhs.type.checkAssignCompatible(rhs.location, rhs.type)
    }
}
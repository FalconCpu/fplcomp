package frontend

class AstAssign(
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

    override fun typeCheck(context:AstBlock) : TcStmt {
        val lhs = lhs.typeCheckLvalue(context)
        val rhs = rhs.typeCheck(context)
        lhs.type.checkAssignCompatible(rhs.location, rhs.type)

        val lhsSym = lhs.getSmartCastSymbol()
        if (lhsSym!=null) {
            if (lhsSym.type != rhs.type)
                currentPathContext = currentPathContext.addSmartCast(lhsSym, rhs.type)
            else
                currentPathContext = currentPathContext.removeSmartcast(lhsSym)
        }
        return TcAssign(location, lhs, rhs)
    }

}

class TcAssign(
    location: Location,
    private val lhs: TcExpr,
    private val rhs: TcExpr
) : TcStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ASSIGN\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
    }


    override fun codeGen() {
        val rhs = rhs.codeGenRvalue()
        lhs.codeGenLvalue(rhs)
    }
}

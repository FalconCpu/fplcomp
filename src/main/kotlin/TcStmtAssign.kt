package falcon

class TcStmtAssign(
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

}
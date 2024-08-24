package falcon

class TcExprBinop(
    location: Location,
    type: Type,
    private val op: AluOp,
    private val lhs: TcExpr,
    private val rhs: TcExpr
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("BINOP $op $type\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
    }
}
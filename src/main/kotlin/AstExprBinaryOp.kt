package falcon

class AstExprBinaryOp(
    location: Location,
    private val op: TokenKind,
    private val lhs: AstExpr,
    private val rhs: AstExpr
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("BINARYOP $op\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
    }
}
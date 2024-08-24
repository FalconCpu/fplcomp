package falcon

class AstExprStringLit(
    location: Location,
    private val value: String
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("STRINGLIT $value\n")
    }

    override fun typeCheckRvalue(symbolTable: SymbolTable): TcExpr {
        return TcExprLiteral(location, StringType, StringValue(value))
    }

}
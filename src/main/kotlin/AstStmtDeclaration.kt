package falcon

class AstStmtDeclaration (
    location: Location,
    private val op : TokenKind,
    private val name: String,
    private val astType: AstType?,
    private val astValue: AstExpr?
) : AstStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("$op $name\n")
        astType?.dump(sb, indent + 1)
        astValue?.dump(sb, indent + 1)
    }

    override fun typeCheck(context: TcBlock) {
        val value = astValue?.typeCheckRvalue(context.symbolTable)
        val type =  astType?.resolveType(context) ?:
                    value?.type ?:
                    makeTypeError(location, "Unknown type for $name")
        val mutable = (op == TokenKind.VAR)

        val symbol : Symbol
        val lhs : TcExpr
        if (context is TcTop) {
            symbol = SymbolGlobalVar(location, name, type, mutable)
            lhs = TcExprGlobalVar(location, symbol)
        } else {
            symbol = SymbolLocalVar(location, name, type, mutable)
            lhs = TcExprLocalVar(location, symbol)
        }
        context.symbolTable.add(symbol)

        if (value != null) {
            type.checkAssignCompatible(value.location, value.type)
            context.add(TcStmtAssign(location, lhs, value))
        }
    }

}
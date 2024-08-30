package frontend

class AstDeclaration (
    location: Location,
    private val op : TokenKind,
    private val name: String,
    private val type: AstType?,
    private val value: AstExpr?
) : AstStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("$op $name\n")
        type?.dump(sb, indent + 1)
        value?.dump(sb, indent + 1)
    }

    override fun typeCheck(context:AstBlock) : TcStmt {
        val value = value?.typeCheck(context)
        val type =  type?.resolveType(context) ?:
                    value?.type ?:
                    makeTypeError(location, "Unknown type for $name")
        val mutable = (op == TokenKind.VAR)

        val symbol = when (context) {
            is AstTop   -> SymbolGlobalVar(location, name, type, mutable)
            is AstClass -> SymbolField(location, name, type, mutable)
            else             -> SymbolLocalVar(location, name, type, mutable)
        }
        context.add(symbol)

        if (value != null)
            type.checkAssignCompatible(value.location, value.type)
        else
            currentPathContext = currentPathContext.addUninitializedVariable(symbol)
        return TcDeclaration(location, symbol, value)
    }

}

class TcDeclaration (
    location: Location,
    private val symbol: Symbol,
    private val value: TcExpr?
) : TcStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("DECL ${symbol.description()} ${symbol.type}\n")
        value?.dump(sb, indent + 1)
    }

    override fun codeGen() {
        if (value==null)
            return

        val rhs = value.codeGenRvalue()

        when(symbol) {
            is SymbolLocalVar -> currentFunction.instrMove( currentFunction.getReg(symbol), rhs)
            is SymbolField -> currentFunction.instrStore(rhs, currentFunction.thisReg!!, symbol)
            is SymbolGlobalVar -> currentFunction.instrStore(rhs, symbol)
            is SymbolFunctionName,
            is SymbolLiteral,
            is SymbolMemberAccess,
            is SymbolTypeName -> error("Got ${this.javaClass} in AstDeclaration")
        }

    }
}

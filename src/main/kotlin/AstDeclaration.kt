package falcon

class AstDeclaration (
    location: Location,
    private val op : TokenKind,
    private val name: String,
    private val type: AstType?,
    private val value: AstExpr?
) : AstStmt(location) {
    lateinit var symbol : Symbol


    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("$op $name\n")
        type?.dump(sb, indent + 1)
        value?.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("DECL ${symbol.description()} ${symbol.type}\n")
        value?.dumpWithType(sb, indent + 1)
    }


    override fun typeCheck(context:AstBlock) {
        value?.typeCheck(context)
        val type =  type?.resolveType(context) ?:
                    value?.type ?:
                    makeTypeError(location, "Unknown type for $name")
        val mutable = (op == TokenKind.VAR)

        symbol = when (context) {
            is AstTop   -> SymbolGlobalVar(location, name, type, mutable)
            is AstClass -> SymbolField(location, name, type, mutable)
            else             -> SymbolLocalVar(location, name, type, mutable)
        }
        context.add(symbol)

        if (value != null)
            type.checkAssignCompatible(value.location, value.type)
        else
            currentPathContext = currentPathContext.addUninitializedVariable(symbol)
    }
}
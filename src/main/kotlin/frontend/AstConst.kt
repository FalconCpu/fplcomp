package frontend

class AstConst(
    location: Location,
    val name : String,
    val value: AstExpr
) : AstStmt(location) {

    override fun typeCheck(context: AstBlock): TcStmt {
        val value = value.typeCheck(context)
        if (!value.hasConstantValue())
            Log.error(location, "value of const must be constant")
        val v = value.getConstantValue()

        val sym = SymbolLiteral(location, name, value.type, v)
        context.add(sym)
        return TcConst(location, name, value)
    }

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CONST $name\n")
        value.dump(sb, indent + 1)
    }
}

class TcConst(
    location: Location,
    val name: String,
    val value: TcExpr
) : TcStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CONST $name\n")
        value.dump(sb, indent + 1)
    }

    override fun codeGen() {
        // Does nothing
    }
}

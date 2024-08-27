package frontend

class AstIfClause(
    location: Location,
    parent : AstBlock,
    val condition: AstExpr?,
) : AstBlock(location, parent) {

    lateinit var pathContextIn : PathContext
    var tcCondition : TcExpr? = null   // Filled in by typeCheckCondition

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CLAUSE\n")
        condition?.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    fun typeCheckCondition(context: AstBlock) {
        val tc = condition?.typeCheck(context)
        if (tc != null)
            BoolType.checkAssignCompatible(tc.location, tc.type)
        tcCondition = tc
    }

    fun typeCheckBody(context:AstBlock) : TcIfClause {
        val ret = TcIfClause(location, symbolTable, tcCondition)
        for (statement in body)
            ret.add( statement.typeCheck(this) )
        return ret
    }

    override fun typeCheck(context: AstBlock): TcStmt {
        error("AstIfClause should never be typecheked directly")
    }
}

class TcIfClause(
    location: Location,
    symbolTable: SymbolTable,
    val condition: TcExpr?
) : TcBlock(location, symbolTable) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CLAUSE\n")
        condition?.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun codeGen() {
        for(statement in body)
            statement.codeGen()
    }


}

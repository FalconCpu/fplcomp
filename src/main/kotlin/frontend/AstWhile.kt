package frontend

class AstWhile(
    location: Location,
    parent: AstBlock,
    val condition: AstExpr,
) : AstBlock(location, parent) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("WHILE\n")
        condition.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("WHILE\n")
        condition.dumpWithType(sb, indent + 1)
        for (stmt in body)
            stmt.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        trueBranchContext = currentPathContext
        falseBranchContext = currentPathContext
        condition.typeCheck(context)
        BoolType.checkAssignCompatible(location, condition.type)

        val endContext = falseBranchContext  // Save the context for the end of the loop
        currentPathContext = trueBranchContext

        for(statement in body)
            statement.typeCheck(this)

        currentPathContext = endContext
    }

}
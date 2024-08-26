package frontend

class AstRepeat(
    location: Location,
    parent : AstBlock
) : AstBlock(location, parent) {

    lateinit var condition : AstExpr

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("REPEAT\n")
        condition.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("REPEAT\n")
        condition.dumpWithType(sb, indent + 1)
        for (stmt in body)
            stmt.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        for (stmt in body)
            stmt.typeCheck(this)

        trueBranchContext = currentPathContext
        falseBranchContext = currentPathContext
        condition.typeCheck(context)
        BoolType.checkAssignCompatible(location, condition.type)
        currentPathContext = falseBranchContext
    }

}
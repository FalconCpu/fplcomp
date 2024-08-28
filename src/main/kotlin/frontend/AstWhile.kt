package frontend

class AstWhile(
    location: Location,
    parent: AstBlock,
    private val condition: AstExpr,
) : AstBlock(location, parent) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("WHILE\n")
        condition.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) : TcBlock {
        trueBranchContext = currentPathContext
        falseBranchContext = currentPathContext
        val condition = condition.typeCheck(context)
        BoolType.checkAssignCompatible(location, condition.type)

        val endContext = falseBranchContext  // Save the context for the end of the loop
        currentPathContext = trueBranchContext

        val ret = TcWhile(location,  condition)

        for(stmt in body)
            ret.add( stmt.typeCheck(this))

        currentPathContext = endContext
        return ret
    }
}

class TcWhile(
    location: Location,
    val condition: TcExpr,
) : TcBlock(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("WHILE\n")
        condition.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun codeGen() {
        val labStart = currentFunction.newLabel()
        val labCond = currentFunction.newLabel()
        val labEnd = currentFunction.newLabel()
        currentFunction.instrJump(labCond)
        currentFunction.instrLabel(labStart)
        for(stmt in body)
            stmt.codeGen()
        currentFunction.instrLabel(labCond)
        condition.codeGenBool(labStart,labEnd)
        currentFunction.instrLabel(labEnd)
    }

}

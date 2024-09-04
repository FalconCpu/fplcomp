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

    override fun typeCheck(context: AstBlock) : TcBlock  {
        val body = body. map { it.typeCheck(this) }

        trueBranchContext = currentPathContext
        falseBranchContext = currentPathContext
        val condition = condition.typeCheck(context)
        BoolType.checkAssignCompatible(location, condition.type)
        currentPathContext = falseBranchContext

        val ret = TcRepeat(location, condition)
        body.forEach { ret.add(it) }
        return ret
    }

}

class TcRepeat(
    location: Location,
    private val condition: TcExpr
) : TcBlock(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("REPEAT\n")
        condition.dump(sb, indent + 1)
        for (stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun codeGen() {
        val labelStart = currentFunction.newLabel()
        val labelEnd = currentFunction.newLabel()

        currentFunction.instrLabel(labelStart)
        body.forEach { it.codeGen() }
        condition.codeGenBool(labelEnd, labelStart)
        currentFunction.instrLabel(labelEnd)
    }

}

package frontend

class AstReturn(
    location: Location,
    private val value: AstExpr?
) : AstStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("RETURN\n")
        value?.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("RETURN\n")
        value?.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        val func = context.findEnclosingFunction() ?:
            return Log.error(location, "Return statement not in function")

        value?.typeCheck(context)
        val valueType = value?.type ?: UnitType
        currentPathContext = currentPathContext.setUnreachable()

        if (value==null && func.retType != UnitType)
            return Log.error(location, "backend.Function should return a value of type ${func.retType}")

        func.retType.checkAssignCompatible(location, valueType)
    }

    override fun codeGen() {
        TODO("Not yet implemented")
    }
}
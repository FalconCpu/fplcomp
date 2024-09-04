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

    override fun typeCheck(context: AstBlock) : TcStmt {
        val value = value?.typeCheck(context)
        val valueType = value?.type ?: UnitType
        currentPathContext = currentPathContext.setUnreachable()

        if (value==null) {
            if (enclosingFunction.returnType != UnitType)
                Log.error(location,"Function ${enclosingFunction.name} should return a value of type ${enclosingFunction.returnType}")
        } else
            enclosingFunction.returnType.checkAssignCompatible(location, value)
        return TcReturn(location, value)
    }

}

class TcReturn(
    location: Location,
    private val value: TcExpr?
) : TcStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("RETURN\n")
        value?.dump(sb, indent + 1)
    }


    override fun codeGen() {
        if (value!= null  && value.type != UnitType) {
            val value = value.codeGenRvalue()
            currentFunction.instrMove(backend.regResult, value)
        }
        currentFunction.instrJump(currentFunction.endLabel)
    }

}

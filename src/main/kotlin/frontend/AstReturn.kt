package frontend

import backend.TupleReg
import backend.allMachineRegs

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
            if (value is TupleReg) {
                // Pass tuples in registers %8, %7, %6 and so on. Tuples are limited to 4 elements.
                check(value.regs.size <= 4)
                for (index in value.regs.indices)
                    currentFunction.instrMove(allMachineRegs[8 - index], value.regs[index])
            } else
                // For other types, pass the value in %8
                currentFunction.instrMove(backend.regResult, value)
        }
        currentFunction.instrJump(currentFunction.endLabel)
    }
}

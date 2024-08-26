package frontend

import backend.Label
import backend.Reg

class AstAnd(
    location: Location,
    private val lhs: AstExpr,
    private val rhs: AstExpr
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("AND\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("AND $type\n")
        lhs.dumpWithType(sb, indent + 1)
        rhs.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        // typeCheck() reads state from global variable currentPathContext, and sets trueBranchContext and falseBranchContext
        trueBranchContext = currentPathContext
        falseBranchContext = currentPathContext
        lhs.typeCheck(context)
        BoolType.checkAssignCompatible(location, lhs.type)
        val midFalseContext = falseBranchContext

        currentPathContext = trueBranchContext
        falseBranchContext = currentPathContext
        rhs.typeCheck(context)
        BoolType.checkAssignCompatible(location, rhs.type)
        falseBranchContext =
            mergePathContext(listOf(midFalseContext, falseBranchContext))

        type = BoolType
    }

    override fun codeGenRvalue(): Reg {
        TODO("Not yet implemented")
    }

    override fun codeGenBool(trueLabel: Label, falseLabel: Label) {
        val label = currentFunction.newLabel()
        lhs.codeGenBool(label, falseLabel)
        currentFunction.instrLabel(label)
        rhs.codeGenBool(trueLabel, falseLabel)
    }
}
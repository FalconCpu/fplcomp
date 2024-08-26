package frontend

import backend.Label
import backend.Reg

class AstOr(
    location: Location,
    private val lhs: AstExpr,
    private val rhs: AstExpr
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("OR\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("OR $type\n")
        lhs.dumpWithType(sb, indent + 1)
        rhs.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        // typeCheck() will update global variables trueBranchContext and falseBranchContext
        trueBranchContext = currentPathContext
        falseBranchContext = currentPathContext
        lhs.typeCheck(context)
        BoolType.checkAssignCompatible(location, lhs.type)
        val midTrueContext = trueBranchContext

        currentPathContext = falseBranchContext
        trueBranchContext = currentPathContext
        rhs.typeCheck(context)
        BoolType.checkAssignCompatible(location, rhs.type)
        trueBranchContext =
            mergePathContext(listOf(midTrueContext, trueBranchContext))

        type = BoolType
    }

    override fun codeGenRvalue(): Reg {
        TODO("Not yet implemented")
    }

    override fun codeGenBool(trueLabel: Label, falseLabel: Label) {
        val label = currentFunction.newLabel()
        lhs.codeGenBool(trueLabel, label)
        currentFunction.instrLabel(label)
        rhs.codeGenBool(trueLabel, falseLabel)
    }

}
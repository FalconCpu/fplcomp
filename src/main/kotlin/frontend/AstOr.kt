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


    override fun typeCheck(context: AstBlock) : TcExpr {
        // typeCheck() will update global variables trueBranchContext and falseBranchContext
        trueBranchContext = currentPathContext
        falseBranchContext = currentPathContext
        val lhs = lhs.typeCheck(context)
        BoolType.checkAssignCompatible(location, lhs.type)
        val midTrueContext = trueBranchContext

        currentPathContext = falseBranchContext
        trueBranchContext = currentPathContext
        val rhs = rhs.typeCheck(context)
        BoolType.checkAssignCompatible(location, rhs.type)
        trueBranchContext =
            mergePathContext(listOf(midTrueContext, trueBranchContext))

        return TcOr(location, lhs, rhs)
    }

}

class TcOr(
    location: Location,
    private val lhs: TcExpr,
    private val rhs: TcExpr
) : TcExpr(location, BoolType) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("OR $type\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
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

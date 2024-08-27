package frontend

import backend.AluOp
import backend.Label
import backend.Reg

class AstNot(
    location: Location,
    private val expr: AstExpr
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("NOT\n")
        expr.dump(sb, indent + 1)
    }


    override fun typeCheck(context: AstBlock) : TcExpr {
        val expr = expr.typeCheck(context)
        BoolType.checkAssignCompatible(location, expr.type)

        val tnp = trueBranchContext
        trueBranchContext = falseBranchContext
        falseBranchContext = tnp
        return TcNot(location, expr)
    }



}

class TcNot(
    location: Location,
    private val expr: TcExpr
) : TcExpr(location, BoolType) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("NOT $type\n")
        expr.dump(sb, indent + 1)
    }


    override fun codeGenRvalue(): Reg {
        val v = expr.codeGenRvalue()
        return currentFunction.instrAlu(AluOp.XOR_I, v, 1)
    }

    override fun codeGenBool(trueLabel: Label, falseLabel: Label) {
        expr.codeGenBool(falseLabel, trueLabel)
    }

}

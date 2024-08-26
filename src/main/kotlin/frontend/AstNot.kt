package frontend

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

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("NOT $type\n")
        expr.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        expr.typeCheck(context)
        BoolType.checkAssignCompatible(location, expr.type)

        val tnp = trueBranchContext
        trueBranchContext = falseBranchContext
        falseBranchContext = tnp
        type = BoolType
    }

    override fun codeGenRvalue(): Reg {
        TODO("Not yet implemented")
    }
}
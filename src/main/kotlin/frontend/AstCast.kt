package frontend

import backend.Reg

class AstCast(
    location: Location,
    private val expr: AstExpr,
    private val astType: AstType
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CAST\n")
        expr.dump(sb, indent + 1)
        astType.dump(sb, indent + 1)
    }


    override fun typeCheck(context: AstBlock) : TcExpr {
        val expr = expr.typeCheck(context)
        val type = astType.resolveType(context)
        return TcCast(location, type, expr)
    }

}

class TcCast(
    location: Location,
    type : Type,
    private val expr: TcExpr
) : TcExpr(location,type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CAST $type\n")
        expr.dump(sb, indent + 1)
    }

    override fun codeGenRvalue(): Reg {
        return expr.codeGenRvalue()
    }

    override fun hasConstantValue() = expr.hasConstantValue()
    override fun getConstantValue() = expr.getConstantValue()
}

package frontend

import backend.Reg

class AstIntLiteral(
    location: Location,
    val type : Type,
    private val value: Int
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("INTLIT $value\n")
    }

    override fun typeCheck(context:AstBlock) : TcExpr {
        return TcIntLiteral(location, type, value)
    }

}

class TcIntLiteral(
    location: Location,
    type : Type,
    private val value: Int
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("INTLIT $value $type\n")
    }

    override fun codeGenRvalue(): Reg {
        return currentFunction.instrInt(value)
    }

    override fun hasConstantValue() = true

    override fun getConstantValue() = value
}

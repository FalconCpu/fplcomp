package frontend

import backend.Reg

class AstIntLiteral(
    location: Location,
    private val value: Int
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("INTLIT $value\n")
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("INTLIT $value $type\n")
    }

    override fun typeCheck(context:AstBlock) {
        type = IntType
    }

    override fun codeGenRvalue(): Reg {
        return currentFunction.instrInt(value)
    }
}
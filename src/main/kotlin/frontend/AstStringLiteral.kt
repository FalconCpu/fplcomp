package frontend

import backend.Reg

class AstStringLiteral(
    location: Location,
    private val value: String
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("STRINGLIT $value\n")
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("STRINGLIT $value $type\n")
    }

    override fun typeCheck(context:AstBlock) {
        type = StringType
    }

    override fun codeGenRvalue(): Reg {
        return currentFunction.instrLea(backend.StringValue(value))
    }
}
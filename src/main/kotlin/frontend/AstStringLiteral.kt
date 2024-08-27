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


    override fun typeCheck(context:AstBlock) : TcExpr{
        return TcStringLiteral(location, value)
    }

}

class TcStringLiteral(
    location: Location,
    private val value: String
) : TcExpr(location, StringType) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("STRINGLIT $value $type\n")
    }

    override fun codeGenRvalue(): Reg {
        return currentFunction.instrLea(backend.StringValue(value))
    }

}

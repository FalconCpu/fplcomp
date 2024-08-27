package frontend

import backend.Reg

class TcError(location: Location, message:String) : TcExpr(location, ErrorType) {
    init {
        Log.error(location, message)
    }

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ERROR\n")
    }

    override fun codeGenRvalue(): Reg {
        error("Code generation of error expression")
    }
}
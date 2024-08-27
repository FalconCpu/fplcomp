package frontend

import backend.Reg

class AstArrayAccess(
    location: Location,
    private val lhs: AstExpr,
    private val index: AstExpr
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYACCESS\n")
        lhs.dump(sb, indent + 1)
        index.dump(sb, indent + 1)
    }


    override fun typeCheck(context: AstBlock) : TcExpr {
        val lhs = lhs.typeCheck(context)
        val index = index.typeCheck(context)

        if (lhs.type is ErrorType) return lhs
        if (index.type is ErrorType) return index

        if (lhs.type !is ArrayType)
            return TcError(location, "Cannot index into type '${lhs.type}'")

        IntType.checkAssignCompatible(index.location, index.type)
        return TcArrayAccess(location, lhs.type.elementType, lhs, index)
    }

}

class TcArrayAccess(
    location: Location,
    type: Type,
    private val lhs: TcExpr,
    private val index: TcExpr
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYACCESS $type\n")
        lhs.dump(sb, indent + 1)
        index.dump(sb, indent + 1)
    }

    override fun codeGenRvalue(): Reg {
        val addr = lhs.codeGenRvalue()
        val index = index.codeGenRvalue()
        return currentFunction.instrLoad(type.getSize(), addr, index)
    }

}

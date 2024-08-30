package frontend

import backend.Reg
import backend.StdlibMallocArray
import backend.regArg1

class AstArrayConstructor (
    location: Location,
    private val astElementType: AstType,
    private val size: AstExpr
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYCONSTRUCTOR\n")
        astElementType.dump(sb, indent + 1)
        size.dump(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) : TcExpr {
        val elementType = astElementType.resolveType(context)
        val size = size.typeCheck(context)
        IntType.checkAssignCompatible (location, size.type)
        val type = makeArrayType(elementType)
        return TcArrayConstructor(location, type, elementType, size)
    }

}

class TcArrayConstructor (
    location: Location,
    type : Type,
    private val elementType : Type,
    private val size: TcExpr
) : TcExpr(location, type ) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYCONSTRUCTOR $type\n")
        size.dump(sb, indent + 1)
    }

    override fun codeGenRvalue(): Reg {
        currentFunction.instrMove(regArg1, size.codeGenRvalue())
        return currentFunction.instrCall(StdlibMallocArray)
    }

}

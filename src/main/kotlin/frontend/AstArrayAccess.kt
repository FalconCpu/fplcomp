package frontend

import backend.AluOp
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

        IntType.checkAssignCompatible(index.location, index.type)
        return when(lhs.type) {
            is ArrayType -> TcArrayAccess(location, lhs.type.elementType, lhs, index)
            is StringType -> TcArrayAccess(location, CharType, lhs, index)
            else -> TcError(location, "Cannot index into type '${lhs.type}'")
        }
    }

    override fun typeCheckLvalue(context: AstBlock): TcExpr {
        val ret = typeCheck(context)
        if (ret is TcArrayAccess && ret.isImmutable())
            Log.error(location, "Cannot write to immutable array")
        return ret
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
        val size = type.getSize()
        val indexScaled = when(size) {
            1 -> index
            2 -> currentFunction.instrAlu(AluOp.LSL_I, index, 1)
            4 -> currentFunction.instrAlu(AluOp.LSL_I, index, 2)
            else -> error("Invalid array size $size")
        }
        val addrFinal = currentFunction.instrAlu(AluOp.ADD_I, addr, indexScaled)

        return currentFunction.instrLoad(type.getSize(), addrFinal, 0)
    }

    fun isImmutable() : Boolean {
        // Do not allow assignment to immutable array
        val lhsType = lhs.type
        return lhsType is ArrayType && !lhsType.mutable
    }

    override fun codeGenLvalue(reg: Reg) {
        val addr = lhs.codeGenRvalue()
        val index = index.codeGenRvalue()
        val size = type.getSize()
        val indexScaled = when(size) {
            1 -> index
            2 -> currentFunction.instrAlu(AluOp.LSL_I, index, 1)
            4 -> currentFunction.instrAlu(AluOp.LSL_I, index, 2)
            else -> error("Invalid array size $size")
        }
        val addrFinal = currentFunction.instrAlu(AluOp.ADD_I, addr, indexScaled)

        return currentFunction.instrStore(type.getSize(), reg, addrFinal, 0)
    }



}

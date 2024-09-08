package frontend

import backend.AluOp
import backend.Reg
import backend.StdlibMalloc
import backend.regArg1
import backend.regArg2

class AstArrayConstructor (
    location: Location,
    private val astElementType: AstType,
    private val size: AstExpr,
    private val mutable : Boolean
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYCONSTRUCTOR $mutable\n")
        astElementType.dump(sb, indent + 1)
        size.dump(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) : TcExpr {
        val elementType = astElementType.resolveType(context)
        val size = size.typeCheck(context)
        IntType.checkAssignCompatible (location, size.type)
        val type = makeArrayType(elementType, mutable)
        return TcArrayConstructor(location, type, elementType, size)
    }

}

class TcArrayConstructor (
    location: Location,
    type : Type,
    private val elementType : Type,
    private val size: TcExpr
) : TcExpr(location, type ) {

    var localAlloc = false

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYCONSTRUCTOR $type\n")
        size.dump(sb, indent + 1)
    }

    override fun codeGenRvalue(): Reg {
        if (localAlloc) {
            if (! size.hasConstantValue())
                Log.error(location, "Size of array must be constant for local allocation")
            val numElements = size.getConstantValue()
            val ret = currentFunction.alloca(4, numElements * type.getSize())
            val ne = currentFunction.instrInt(numElements)
            currentFunction.instrStore(ne, ret, sizeSymbol)
            return ret
        } else {
            val numElements = size.codeGenRvalue()
            val sizeInBytes = currentFunction.instrAlu(AluOp.MUL_I, numElements, elementType.getSize())
            currentFunction.instrMove(regArg1, sizeInBytes)
            currentFunction.instrLea(regArg2, type)
            val ret = currentFunction.instrCall(StdlibMalloc)
            currentFunction.instrStore(numElements, ret, sizeSymbol)
            return ret
        }
    }

}

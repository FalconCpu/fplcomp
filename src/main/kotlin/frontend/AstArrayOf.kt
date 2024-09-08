package frontend

import backend.ArrayRefValue
import backend.InstrLit
import backend.Reg
import backend.StdlibMalloc
import sun.net.www.content.text.PlainTextInputStream

val allConstantArrays = mutableListOf<ArrayImage>()

class AstArrayOf(
    location: Location,
    private val astElementType: AstType?,
    private val elements : List<AstExpr>,
    private val mutable: Boolean
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYOF\n")
        astElementType?.dump(sb, indent + 1)
        for (element in elements)
            element.dump(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) : TcExpr {
        val elements = elements.map { it.typeCheck(context) }

        if (elements.isEmpty() && astElementType == null)
            return TcError(location, "Array of unknown type")

        val elementType = astElementType?.resolveType(context) ?: elements[0].type

        for (element in elements)
            elementType.checkAssignCompatible (element.location, element)

        val type = makeArrayType(elementType, mutable)
        return TcArrayOf(location, type, elementType, elements)
    }

}

class TcArrayOf(
    location: Location,
    type: Type,
    private val elementType: Type,
    private val elements : List<TcExpr>
) : TcExpr(location, type) {

    var localAlloc = false

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYOF $type\n")
        for (element in elements)
            element.dump(sb, indent + 1)
    }

    override fun codeGenRvalue(): Reg {
        val numElements = elements.size
        val elementSize = elementType.getSize()
        val immutable = type is ArrayType && !type.mutable

        // If we can build the array at compile time, do it
        if (immutable && elements.all{ it.hasConstantValue()}) {
            val values = elements.map { it.getConstantValue() }
            val image = ArrayImage(allConstantArrays.size, elementType, values)
            allConstantArrays.add(image)
            return currentFunction.instrLea(image)
        }

        val ret : Reg
        if (localAlloc) {
            ret = currentFunction.alloca(4, numElements * elementSize)   // 4 for the length field
        } else {
            currentFunction.instrMove(backend.regArg1, numElements * elementSize)
            currentFunction.instrLea(backend.regArg2, type)
            ret = currentFunction.instrCall(StdlibMalloc)
        }
        val numEl = currentFunction.instrInt(numElements)
        currentFunction.instrStore(numEl, ret, sizeSymbol)

        for ((index, element) in elements.withIndex()) {
            val value = element.codeGenRvalue()
            currentFunction.instrStore(elementSize, value, ret, index * elementSize)
        }
        return ret
    }
}

class ArrayImage(
    val id : Int,
    val elementType: Type,
    val elements: List<Int>
) {
    override fun toString() = "Array|$id"

    fun asmGen(sb: StringBuilder) {
        sb.append("dcw ${elements.size}\n")
        sb.append("Array|$id:\n")
        if (elementType is CharType) {
            for(e in elements.chunked(4))
                sb.append("dcb ${e.joinToString(",")}\n")
        } else {
            for (e in elements)
                sb.append("dcw $e\n")
        }
        sb.append("\n")
    }


}
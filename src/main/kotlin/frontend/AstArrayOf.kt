package frontend

import backend.InstrLit
import backend.Reg
import backend.StdlibMallocArray

class AstArrayOf(
    location: Location,
    private val astElementType: AstType?,
    private val elements : List<AstExpr>
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
            elementType.checkAssignCompatible (element.location, element.type)

        val type = makeArrayType(elementType)
        return TcArrayOf(location, type, elementType, elements)
    }

}

class TcArrayOf(
    location: Location,
    type: Type,
    private val elementType: Type,
    private val elements : List<TcExpr>
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYOF $type\n")
        for (element in elements)
            element.dump(sb, indent + 1)
    }

    override fun codeGenRvalue(): Reg {
        val numElements = elements.size
        val elementSize = elementType.getSize()
        currentFunction.add(InstrLit(backend.regArg1, numElements))
        currentFunction.add(InstrLit(backend.regArg2, elementSize))
        val ret = currentFunction.instrCall(StdlibMallocArray)
        for ((index, element) in elements.withIndex()) {
            val value = element.codeGenRvalue()
            currentFunction.instrStore(elementSize, value, ret, index)
        }
        return ret
    }

}

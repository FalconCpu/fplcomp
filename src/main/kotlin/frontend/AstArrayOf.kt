package frontend

import backend.InstrLit
import backend.Reg
import backend.StdlibMallocArray

class AstArrayOf(
    location: Location,
    private val astElementType: AstType?,
    private val elements : List<AstExpr>
) : AstExpr(location) {

    lateinit var elementType : Type

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYOF\n")
        astElementType?.dump(sb, indent + 1)
        for (element in elements)
            element.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ARRAYOF $type\n")
        for (element in elements)
            element.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        elements.forEach { it.typeCheck(context) }

        if (elements.isEmpty() && astElementType == null)
            return setTypeError("Array of unknown type")

        elementType = astElementType?.resolveType(context) ?: elements[0].type

        for (element in elements)
            elementType.checkAssignCompatible (element.location, element.type)

        type = makeArrayType(elementType)
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
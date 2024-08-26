package frontend

import backend.Reg

class AstNullAccess(
    location: Location,
    private val lhs: AstExpr,
    private val name: String
) : AstExpr(location) {

    lateinit var symbol: Symbol

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("NULLACCESS $name\n")
        lhs.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("NULLACCESS $name $type\n")
        lhs.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        lhs.typeCheck(context)
        if (lhs.type is ErrorType)
            return setTypeError()
        if (lhs.type !is NullableType)
            return setTypeError("Not a nullable type '${lhs.type}'")

        val lhsType = lhs.type.makeNonNull()
        if (lhsType !is ClassType)
            return setTypeError("Cannot access field $name of non-class type $lhsType")

        symbol = lhsType.definition.lookupNoHierarchy(name)
            ?: return setTypeError("Class '$lhsType' has no field named '$name'")

        check(symbol is SymbolField || symbol is SymbolFunctionName || symbol is SymbolLiteral)
        type = makeNullableType(symbol.type)
    }

    override fun codeGenRvalue(): Reg {
        TODO("Not yet implemented")
    }
}
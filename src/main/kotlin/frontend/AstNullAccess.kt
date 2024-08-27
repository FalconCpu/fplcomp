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


    override fun typeCheck(context: AstBlock) : TcExpr {
        val lhs = lhs.typeCheck(context)
        if (lhs.type is ErrorType)
            return lhs
        if (lhs.type !is NullableType)
            return TcError(location, "Not a nullable type '${lhs.type}'")

        val lhsType = lhs.type.makeNonNull()
        if (lhsType !is ClassType)
            return TcError(location, "Cannot access field $name of non-class type $lhsType")

        symbol = lhsType.definition.lookupNoHierarchy(name)
            ?: return TcError(location, "Class '$lhsType' has no field named '$name'")

        check(symbol is SymbolField || symbol is SymbolFunctionName || symbol is SymbolLiteral)
        val type = makeNullableType(symbol.type)
        return TcNullAccess(location, type, lhs, symbol)
    }

}

class TcNullAccess(
    location: Location,
    type: Type,
    private val lhs: TcExpr,
    private val symbol : Symbol
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("NULLACCESS $symbol $type\n")
        lhs.dump(sb, indent + 1)
    }


    override fun codeGenRvalue(): Reg {
        TODO("Not yet implemented")
    }

}

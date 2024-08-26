package frontend

import backend.Reg

class AstMemberAccess (
    location: Location,
    private val lhs: AstExpr,
    private val name: String
) : AstExpr(location) {

    lateinit var symbol : Symbol

    // For the purpose of smart casting we represent a castable member access as a symbol
    var smartCastSymbol : SymbolMemberAccess? = null    // A M

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("MEMBERACCESS $name\n")
        lhs.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("MEMBERACCESS $name $type\n")
        lhs.dumpWithType(sb, indent + 1)
    }

    override fun isMutable(): Boolean = symbol.isMutable()

    private fun generateSmartCastSymbol(): SymbolMemberAccess? {
        // if this member access has either an immutable symbol or another member access as its lhs
        // then we can potentially track this as a smart cast
        val lhsSymbol =
            if (lhs.isMutable())
                null
            else if (lhs is AstIdentifier)
                lhs.symbol
            else if (lhs is AstMemberAccess)
                lhs.smartCastSymbol
            else
                null

        val smartCastSymbol =
            if (lhsSymbol != null)
                SymbolMemberAccess(location, lhsSymbol, symbol)
            else
                null
        return smartCastSymbol
    }

    override fun typeCheckLvalue(context: AstBlock) {
        typeCheck(context)
    }

    private fun accessEnum(lhsType:EnumType) {
        symbol = lhsType.definition.lookupNoHierarchy(name)
            ?: return setTypeError("Enum '$lhsType' has no field named '$name'")
        type = symbol.type
    }

    private fun accessArray(lhsType:ArrayType) {
        if (name=="size") {
            type = IntType
            symbol = sizeSymbol
            return
        } else {
            setTypeError("Cannot access field $name of array type $lhsType")
        }
    }

    private fun accessClass(lhsType:ClassType) {
        symbol = lhsType.definition.lookupNoHierarchy(name)
            ?: return setTypeError("Class '$lhsType' has no field named '$name'")

        check(symbol is SymbolField || symbol is SymbolFunctionName || symbol is SymbolLiteral)
        val smartCastSymbol = generateSmartCastSymbol()

        val smartCastType = if (smartCastSymbol != null)
            currentPathContext.smartCasts[smartCastSymbol] else null

        type = smartCastType ?:  symbol.type
        this.smartCastSymbol = smartCastSymbol
    }


    override fun typeCheck(context:AstBlock) {
        lhs.typeCheckAllowTypeName(context)
        val lhsType = lhs.type

        return if (lhs.isTypeName()) {
            when (lhsType) {
                is ErrorType -> setTypeError()
                is EnumType -> accessEnum(lhsType)
                else -> setTypeError("Cannot access field $name of type $lhsType")
            }
        } else {
            when (lhsType) {
                is ErrorType -> setTypeError()
                is ClassType -> accessClass(lhsType)
                is NullableType -> setTypeError("Member access is not allowed on nullable type $lhsType")
                is ArrayType -> accessArray(lhsType)
                else -> setTypeError("Cannot access field $name of non-class type $lhsType")
            }
        }
    }

    override fun codeGenRvalue(): Reg {
        val sym = symbol
        check(sym is SymbolField)
        val addr = lhs.codeGenRvalue()
        return currentFunction.instrLoad(type.getSize(), addr, sym)
    }
}

val sizeSymbol = SymbolField(nullLocation, "size", IntType, false)


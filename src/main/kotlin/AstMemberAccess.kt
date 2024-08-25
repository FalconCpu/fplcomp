package falcon

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

    override fun typeCheck(context:AstBlock) {
        lhs.typeCheck(context)
        val lhsType = lhs.type

        if (lhsType is ErrorType)
            return setTypeError()

        // Special case for accessing size of array
        if (lhsType is ArrayType && name=="size") {
            type = IntType
            symbol = sizeSymbol
            return
        }

        if (lhsType is NullableType)
            return setTypeError("Member access is not allowed on nullable type $lhsType")
        if (lhsType !is ClassType)
            return setTypeError("Cannot access field $name of non-class type $lhsType")

        symbol = lhsType.definition.lookupNoHierarchy(name)
            ?: return setTypeError("Class '$lhsType' has no field named '$name'")

        when(symbol) {
            is SymbolField,
            is SymbolFunctionName,
            is SymbolLiteral -> {}
            is SymbolGlobalVar -> error("Internal error: Global variable $name inside class $lhsType")
            is SymbolLocalVar -> Log.error(location,"Cannot access local variable $name inside class $lhsType")
            is SymbolTypeName -> Log.error("Cannot access type name $name inside class $lhsType")
            is SymbolMemberAccess -> error("Internal error: Member access inside class $lhsType")
        }
        val smartCastSymbol = generateSmartCastSymbol()

        val smartCastType = if (smartCastSymbol != null)
            currentPathContext.smartCasts[smartCastSymbol] else null

        type = smartCastType ?:  symbol.type
        this.smartCastSymbol = smartCastSymbol
    }

}

private val sizeSymbol = SymbolField(nullLocation, "size", IntType, false)


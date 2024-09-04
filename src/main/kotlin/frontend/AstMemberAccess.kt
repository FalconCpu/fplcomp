package frontend

import backend.Reg

class AstMemberAccess (
    location: Location,
    private val lhs: AstExpr,
    private val name: String
) : AstExpr(location) {

    // For the purpose of smart casting we represent a castable member access as a symbol

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("MEMBERACCESS $name\n")
        lhs.dump(sb, indent + 1)
    }

    private fun generateSmartCastSymbol(lhs:TcExpr, rhs:Symbol): SymbolMemberAccess? {
        // if this member access has either an immutable symbol or another member access as its lhs
        // then we can potentially track this as a smart cast
        val lhsSymbol =
            if (lhs.isMutable())
                null
            else if (lhs is TcIdentifier)
                lhs.symbol
            else if (lhs is TcMemberAccess)
                lhs.smartCastSymbol
            else
                null

        return if (lhsSymbol != null)
                SymbolMemberAccess(location, lhsSymbol, rhs)
            else
                null
    }

    override fun typeCheckLvalue(context: AstBlock) : TcExpr {
        return typeCheck(context)
    }

    private fun accessEnum(lhsType:EnumType) : TcExpr {
        val symbol = lhsType.definition.lookupNoHierarchy(name)
            ?: return TcError(location, "Enum '$lhsType' has no field named '$name'")
        return TcIdentifier(location, symbol.type, symbol)
    }

    private fun accessArray(lhs:TcExpr) : TcExpr {
        return if (name=="size")
            TcMemberAccess(location, IntType, lhs, sizeSymbol, null)
        else
            TcError(location, "Cannot access field $name of array type ${lhs.type}")
    }

    private fun accessString(lhs:TcExpr) : TcExpr {
        return if (name=="length")
            TcMemberAccess(location, IntType, lhs, sizeSymbol, null)
        else
            TcError(location, "Cannot access field $name of string type ${lhs.type}")
    }


    private fun accessClass(lhs: TcExpr) : TcExpr {
        require(lhs.type is ClassType)
        val symbol = lhs.type.lookup(name)
            ?: return TcError(location,"Class '${lhs.type}' has no field named '$name'")

        check(symbol is SymbolField || symbol is SymbolFunctionName || symbol is SymbolLiteral)
        val smartCastSymbol = generateSmartCastSymbol(lhs,symbol)

        val smartCastType = if (smartCastSymbol != null)
            currentPathContext.smartCasts[smartCastSymbol] else null

        val type = smartCastType ?:  symbol.type
        return TcMemberAccess(location, type, lhs, symbol, smartCastSymbol)
    }

    override fun typeCheck(context:AstBlock) : TcExpr {
        val lhs = lhs.typeCheckAllowType(context)

        return if (lhs.isTypeName()) {
            when (lhs.type) {
                is ErrorType -> lhs
                is EnumType -> accessEnum(lhs.type)
                else -> TcError(location, "Cannot access field $name of type ${lhs.type}")
            }
        } else {
            when (lhs.type) {
                is ErrorType -> lhs
                is ClassType -> accessClass(lhs)
                is NullableType -> TcError(location, "Member access is not allowed on nullable type ${lhs.type}")
                is ArrayType -> accessArray(lhs)
                is StringType -> accessString(lhs)
                else -> TcError(location, "Cannot access field $name of non-class type ${lhs.type}")
            }
        }
    }

}

val sizeSymbol = SymbolField(nullLocation, "size", IntType, false).also{ it.offset=-1}


class TcMemberAccess (
    location: Location,
    type : Type,
    val lhs: TcExpr,
    val symbol: Symbol,
    val smartCastSymbol: SymbolMemberAccess?
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("MEMBERACCESS $symbol $type\n")
        lhs.dump(sb, indent + 1)
    }

    override fun isMutable(): Boolean = symbol.isMutable()

    override fun codeGenRvalue(): Reg {
        val sym = symbol
        check(sym is SymbolField)
        val addr = lhs.codeGenRvalue()
        return currentFunction.instrLoad(addr, sym)
    }


    override fun codeGenLvalue(rhs: Reg) {
        val sym = symbol
        check(sym is SymbolField)
        val addr = lhs.codeGenRvalue()
        currentFunction.instrStore(rhs, addr, sym)
    }

}

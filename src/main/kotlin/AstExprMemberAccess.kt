package falcon

class AstExprMemberAccess (
    location: Location,
    private val lhs: AstExpr,
    private val name: String
) : AstExpr(location) {

    lateinit var symbol : Symbol

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

        if (lhsType !is ClassType)
            return setTypeError("Cannot access field $name of non-class type $lhsType")

        symbol = lhsType.definition.lookupNoHierarchy(name)
            ?: return setTypeError("Class '$lhsType' has no field named '$name'")

        when(symbol) {
            is SymbolField,
            is SymbolFunctionName,
            is SymbolLiteral -> {}
            is SymbolGlobalVar -> error("Internal error: Global variable $name inside class $lhsType")
            is SymbolLocalVar -> Log.error("Cannot access local variable $name inside class $lhsType")
            is SymbolTypeName -> Log.error("Cannot access type name $name inside class $lhsType")
        }

        type = symbol.type
    }

}

private val sizeSymbol = SymbolField(nullLocation, "size", IntType, false)
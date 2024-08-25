package falcon

class AstEquals(
    location: Location,
    private val lhs: AstExpr,
    private val rhs: AstExpr,
    private val eq : Boolean  // true = ==, false = !=
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        if (eq)
            sb.append("EQ\n")
        else
            sb.append("NEQ\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        if (eq)
            sb.append("EQ $type\n")
        else
            sb.append("NEQ $type\n")
        lhs.dumpWithType(sb, indent + 1)
        rhs.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        lhs.typeCheck(context)
        rhs.typeCheck(context)
        if (lhs.type == ErrorType || rhs.type == ErrorType)
            return setTypeError()

        val lhsType = lhs.type
        val rhsType = rhs.type
        val lhsSymbol = if (lhs is AstIdentifier) lhs.symbol else null
        val rhsSymbol = if (rhs is AstIdentifier) rhs.symbol else null

        // Check for different cases, and set up smart casts

        if (lhsType == rhsType) {
            // No Smart Casts

        } else if (lhsType is NullableType && rhsType is NullType) {
            if (lhsSymbol!=null)
                if (eq) {
                    trueBranchContext = currentPathContext.addSmartCast(lhsSymbol, NullType)
                    falseBranchContext = currentPathContext.addSmartCast(lhsSymbol, lhsType.base)
                } else {
                    trueBranchContext = currentPathContext.addSmartCast(lhsSymbol, lhsType.base)
                    falseBranchContext = currentPathContext.addSmartCast(lhsSymbol, NullType)
                }

        } else if (lhsType is NullableType && rhsType == lhsType.base) {
            if (lhsSymbol!=null)
                if (eq)
                    trueBranchContext = currentPathContext.addSmartCast(lhsSymbol, lhsType.base)
                else
                    falseBranchContext = currentPathContext.addSmartCast(lhsSymbol, lhsType.base)

        } else if (rhsType is NullableType && lhsType is NullType) {
            if (rhsSymbol!=null)
                if (eq) {
                    trueBranchContext = currentPathContext.addSmartCast(rhsSymbol, NullType)
                    falseBranchContext = currentPathContext.addSmartCast(rhsSymbol, rhsType.base)
                } else {
                    trueBranchContext = currentPathContext.addSmartCast(rhsSymbol, rhsType.base)
                    falseBranchContext = currentPathContext.addSmartCast(rhsSymbol, NullType)
                }

        } else if (rhsType is NullableType && lhsType == rhsType.base) {
            if (rhsSymbol!=null)
                if (eq)
                    trueBranchContext = currentPathContext.addSmartCast(rhsSymbol, rhsType.base)
                else
                    falseBranchContext = currentPathContext.addSmartCast(rhsSymbol, rhsType.base)
        }
        else
            return setTypeError("Invalid types for equality test: $lhsType and $rhsType")

        type = BoolType

    }

}
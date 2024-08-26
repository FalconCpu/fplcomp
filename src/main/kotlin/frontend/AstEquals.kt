package frontend

import backend.Reg

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

    private fun isAcceptableTypes(a:AstExpr, b:AstExpr): Boolean {
        // Since equality checking is symmetrical, without loss of generality we can assume 'a' is the more complex type
        // We can also assume the operation is an equality check. For not-equals we can just flip the smart casts
        val aType = a.type
        val bType = b.type
        val aSymbol = a.getSmartCastSymbol()

        // If either side is an error then we can just return true to avoid error propagation
        if (aType is ErrorType || bType == ErrorType)
            return true

        if (aType == bType)
            return true

        // Compare a nullable with null
        if (aType is NullableType && bType is NullType) {
            trueBranchContext = currentPathContext.addSmartCast(aSymbol, NullType)
            falseBranchContext = currentPathContext.addSmartCast(aSymbol, aType.base)
            return true
        }

        // Compare a nullable with a non-nullable of the same type
        if (aType is NullableType && bType == aType.base) {
            trueBranchContext = currentPathContext.addSmartCast(aSymbol, aType.base)
            return true
        }

        return false
    }

    override fun typeCheck(context: AstBlock) {
        lhs.typeCheck(context)
        rhs.typeCheck(context)

        trueBranchContext = currentPathContext
        falseBranchContext = currentPathContext

        // Check for different cases, and set up smart casts
        // Allow expressions to be either side of the equality check
        if (!isAcceptableTypes(lhs, rhs) && !isAcceptableTypes(rhs, lhs))
            Log.error(location, "Equality check of incompatible types: ${lhs.type} and ${rhs.type}")

        if (!eq) {
            // If we are doing a not-equals, then we flip the smart casts
            val temp = trueBranchContext
            trueBranchContext = falseBranchContext
            falseBranchContext = temp
        }

        type = BoolType

    }

    override fun codeGenRvalue(): Reg {
        TODO("Not yet implemented")
    }
}
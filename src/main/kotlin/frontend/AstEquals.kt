package frontend

import backend.AluOp
import backend.Label
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

    private fun isAcceptableTypes(a:TcExpr, b:TcExpr): Boolean {
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

    override fun typeCheck(context: AstBlock) : TcExpr {
        val lhs = lhs.typeCheck(context)
        val rhs = rhs.typeCheck(context)

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

        return TcEquals(location, BoolType, lhs, rhs, eq)
    }
}

class TcEquals(
    location: Location,
    type : Type,
    private val lhs: TcExpr,
    private val rhs: TcExpr,
    private val eq : Boolean  // true = ==, false = !=
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        if (eq)
            sb.append("EQ $type\n")
        else
            sb.append("NEQ $type\n")
        lhs.dump(sb, indent + 1)
        rhs.dump(sb, indent + 1)
    }

    override fun codeGenRvalue(): Reg {
        val lhs = lhs.codeGenRvalue()
        val rhs = rhs.codeGenRvalue()
        val op = if (eq) AluOp.EQ_I else AluOp.NE_I
        return currentFunction.instrAlu(op, lhs, rhs)
    }

    override fun codeGenBool(trueLabel: Label, falseLabel: Label) {
        check(lhs.type != RealType && lhs.type != StringType){"TODO REAL AND STRING EQUALS"}
        val lhs = lhs.codeGenRvalue()
        val rhs = rhs.codeGenRvalue()
        val op = if (eq) AluOp.EQ_I else AluOp.NE_I
        currentFunction.instrBranch(op, lhs, rhs, trueLabel)
        currentFunction.instrJump(falseLabel)
    }

}

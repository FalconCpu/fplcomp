package frontend

import backend.Reg

class AstIs (
    location: Location,
    private val lhs: AstExpr,
    private val astType: AstType,
    private val isPositiveCheck : Boolean    // true for is, false for is not
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IS\n")
        lhs.dump(sb, indent + 1)
        astType.dump(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) : TcExpr {
        val lhs = lhs.typeCheck(context)
        val rhsType = astType.resolveType(context)

        if (lhs.type == ErrorType) return lhs

        val lhsType = lhs.type.makeNonNull()

        if (lhsType !is ClassType)
            return TcError(lhs.location, "Cannot use 'is' on non-class type $lhsType")
        if (rhsType !is ClassType)
            return TcError(astType.location, "Cannot use 'is' on non-class type $rhsType")
        if (!rhsType.isSubTypeOf(lhsType))
            return TcError(location, "Type $lhsType is not a subtype of $rhsType")

        val lhsSym = lhs.getSmartCastSymbol()
        if (isPositiveCheck) {
            trueBranchContext = currentPathContext.addSmartCast(lhsSym, rhsType)
            falseBranchContext = currentPathContext
        } else {
            falseBranchContext = currentPathContext.addSmartCast(lhsSym, rhsType)
            trueBranchContext = currentPathContext
        }
        return TcIs(location, BoolType, lhs, rhsType, isPositiveCheck)
    }

}

class TcIs (
    location: Location,
    type: Type,
    private val lhs: TcExpr,
    private val compType: Type,
    private val isPositiveCheck : Boolean    // true for is, false for is not
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IS $compType\n")
        lhs.dump(sb, indent + 1)
    }

    override fun codeGenRvalue(): Reg {
        TODO("Not yet implemented")
    }

}

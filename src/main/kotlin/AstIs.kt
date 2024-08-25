package falcon

class AstIs (
    location: Location,
    private val lhs: AstExpr,
    private val astType: AstType,
    private val isPositiveCheck : Boolean    // true for is, false for is not
) : AstExpr(location) {

    lateinit var rhsType: ClassType

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IS\n")
        lhs.dump(sb, indent + 1)
        astType.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IS $rhsType\n")
        lhs.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        lhs.typeCheck(context)
        val rhsType = astType.resolveType(context)
        type = BoolType

        if (lhs.type == ErrorType || rhsType == ErrorType)
            return

        val lhsType = lhs.type.makeNonNull()

        if (lhsType !is ClassType)
            return Log.error(lhs.location, "Cannot use 'is' on non-class type $lhsType")
        if (rhsType !is ClassType)
            return Log.error(astType.location, "Cannot use 'is' on non-class type $rhsType")
        if (!rhsType.isSubTypeOf(lhsType))
            return Log.error(location, "Type $lhsType is not a subtype of $rhsType")
        this.rhsType = rhsType

        val lhsSym = lhs.getSmartCastSymbol()
        if (isPositiveCheck) {
            trueBranchContext = currentPathContext.addSmartCast(lhsSym, rhsType)
            falseBranchContext = currentPathContext
        } else {
            falseBranchContext = currentPathContext.addSmartCast(lhsSym, rhsType)
            trueBranchContext = currentPathContext
        }
    }
}
package frontend

import backend.Reg

class AstIfExpr(
    location: Location,
    private val condition: AstExpr,
    private val thenBranch: AstExpr,
    private val elseBranch: AstExpr
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IF_EXPR\n")
        condition.dump(sb, indent + 1)
        thenBranch.dump(sb, indent + 1)
        elseBranch.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IF_EXPR $type\n")
        condition.dumpWithType(sb, indent + 1)
        thenBranch.dumpWithType(sb, indent + 1)
        elseBranch.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        // Type-check the condition expression. The call to typeCheck() will update global variables
        // trueBranchContext and falseBranchContext
        trueBranchContext = currentPathContext
        falseBranchContext = currentPathContext
        condition.typeCheck(context)
        BoolType.checkAssignCompatible(location, condition.type)

        // Save the current path context before type-checking the branches
        val originalTrueBranchContext = trueBranchContext
        val originalFalseBranchContext = falseBranchContext

        // Type-check the then branch
        currentPathContext = originalTrueBranchContext
        thenBranch.typeCheck(context)
        val afterTrueBranchContext = currentPathContext

        // Type-check the else branch
        currentPathContext = originalFalseBranchContext
        elseBranch.typeCheck(context)
        val afterFalseBranchContext = currentPathContext

        // Ensure the then and else branches have compatible types
        thenBranch.type.checkAssignCompatible(location, elseBranch.type)

        // Set the type of the if expression to the type of the branches
        type = thenBranch.type

        // Restore the path context to a merged state
        currentPathContext =
            mergePathContext(listOf(afterTrueBranchContext, afterFalseBranchContext))
    }

    override fun codeGenRvalue(): Reg {
        TODO("Not yet implemented")
    }
}
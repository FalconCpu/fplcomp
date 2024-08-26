package frontend

import backend.Label

class AstIf(
    location: Location,
    parent : AstBlock,
    private val clauses : List<AstIfClause>
) : AstBlock(location, parent) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IF\n")
        for (clause in clauses)
            clause.dump(sb, indent + 1)
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IF\n")
        for (clause in clauses)
            clause.dumpWithType(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) {
        // Make a first pass through the clauses to check the conditions and set the pathContextIn for when that cause is taken
        for (clause in clauses) {
            if (clause.condition != null) {
                trueBranchContext =
                    currentPathContext  // These global vars get updated by condition.typeCheck()
                falseBranchContext = currentPathContext
                clause.condition.typeCheck(context)
                BoolType.checkAssignCompatible(clause.location, clause.condition.type)
                clause.pathContextIn = trueBranchContext
                currentPathContext = falseBranchContext
            } else {
                clause.pathContextIn = currentPathContext
            }
        }

        // If there is no else clause, then allow for the fallthrough case
        val mergeContexts = mutableListOf<PathContext>()
        if (clauses.none { it.condition == null })
            mergeContexts.add(currentPathContext)

        // Make a second pass through the clauses to type check the body of each clause and get the path
        // contexts for that case
        for (clause in clauses) {
            currentPathContext = clause.pathContextIn
            clause.typeCheck(clause)
            mergeContexts.add(currentPathContext)
        }

        currentPathContext = mergePathContext(mergeContexts)

    }

    override fun codeGen() {
        val labelEnd = currentFunction.newLabel()
        val clauseLabels = mutableListOf<Label>()

        // Generate the code for the conditions for each clause
        for (clause in clauses) {
            val clauseLabel = currentFunction.newLabel()
            clauseLabels += clauseLabel
            if (clause.condition != null) {
                val nextClause = currentFunction.newLabel()
                clause.condition.codeGenBool(clauseLabel, nextClause)
                currentFunction.instrLabel(nextClause)
            } else
                currentFunction.instrJump(clauseLabel)
        }
        currentFunction.instrLabel(labelEnd)

        // Generate the code for the body of each clause
        for ((index,clause) in clauses.withIndex()) {
            currentFunction.instrLabel(clauseLabels[index])
            clause.codeGen()
            currentFunction.instrJump(labelEnd)
        }

        currentFunction.instrLabel(labelEnd)
    }
}
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

    override fun typeCheck(context: AstBlock) : TcStmt {
        // Make a first pass through the clauses to check the conditions and set the pathContextIn for when that cause is taken
        for (clause in clauses) {
            if (clause.condition != null) {
                trueBranchContext = currentPathContext
                falseBranchContext = currentPathContext
                clause.typeCheckCondition(context)  // updates global vars trueBranchContext and falseBranchContext
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
        val tcClauses = mutableListOf<TcIfClause>()
        for (clause in clauses) {
            currentPathContext = clause.pathContextIn
            tcClauses += clause.typeCheckBody(clause)
            mergeContexts.add(currentPathContext)
        }

        currentPathContext = mergePathContext(mergeContexts)
        return TcIf(location, tcClauses)
    }

}

class TcIf(
    location: Location,
    private val clauses : List<TcIfClause>
) : TcStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IF\n")
        for (clause in clauses)
            clause.dump(sb, indent + 1)
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
        currentFunction.instrJump(labelEnd)

        // Generate the code for the body of each clause
        for ((index,clause) in clauses.withIndex()) {
            currentFunction.instrLabel(clauseLabels[index])
            clause.codeGen()
            currentFunction.instrJump(labelEnd)
        }

        currentFunction.instrLabel(labelEnd)
    }

}

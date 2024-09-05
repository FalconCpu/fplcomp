package frontend

import backend.Reg
import backend.TupleReg
import kotlin.math.exp

class AstTuple(
    location: Location,
    val expressions: List<AstExpr>
) : AstExpr(location) {
    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("TUPLE\n")
        for (expr in expressions)
            expr.dump(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock): TcExpr {
        val expressions = expressions.map { it.typeCheck(context) }
        if (expressions.size > 4)
            Log.error(location, "Tuples can only have up to 4 elements")
        val types = expressions.map { it.type }
        return TcTuple(location, makeTupleType(types), expressions)
    }

    override fun typeCheckLvalue(context: AstBlock): TcExpr {
        val expressions = expressions.map { it.typeCheckLvalue(context) }
        if (expressions.size > 4)
            Log.error(location, "Tuples can only have up to 4 elements")
        val types = expressions.map { it.type }
        return TcTuple(location, makeTupleType(types), expressions)
    }
}

class TcTuple(
    location: Location,
    type : Type,
    val expressions: List<TcExpr>
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("TUPLE\n")
        for (expr in expressions)
            expr.dump(sb, indent + 1)
    }

    override fun codeGenRvalue(): Reg {
        val regs = expressions.map { it.codeGenRvalue() }
        val name = regs.joinToString(prefix = "(", postfix = ")") { it.name }
        return TupleReg(name, regs)
    }

    override fun codeGenLvalue(reg: Reg) {
        check(reg is TupleReg)   // This should have already been checked in the type chcecker
        check(expressions.size == reg.regs.size)

        for((lhs, rhs) in expressions.zip(reg.regs))
            lhs.codeGenLvalue(rhs)
    }
}
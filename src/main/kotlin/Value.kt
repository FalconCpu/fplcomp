sealed class Value()

class IntValue(val value: Int) : Value()  {
    override fun toString() = value.toString()
}

class StringValue(val value: String) : Value() {
    override fun toString() = value
}

class ArrayValue(val value: Array<Value>) : Value()

object UndefinedValue : Value()

fun evaluate(op: AluOp, lhs: IntValue, rhs: IntValue): Value {
    val result = when (op) {
        AluOp.ADD_I -> lhs.value + rhs.value
        AluOp.SUB_I -> lhs.value - rhs.value
        AluOp.MUL_I -> lhs.value * rhs.value
        AluOp.DIV_I -> lhs.value / rhs.value
        AluOp.MOD_I -> lhs.value % rhs.value
        AluOp.AND_I -> lhs.value and rhs.value
        AluOp.OR_I -> lhs.value or rhs.value
        AluOp.XOR_I -> lhs.value xor rhs.value
        AluOp.SHL_I -> lhs.value shl rhs.value
        AluOp.SHR_I -> lhs.value shr rhs.value
        AluOp.EQ_I,
        AluOp.NE_I,
        AluOp.LT_I,
        AluOp.GT_I,
        AluOp.LE_I,
        AluOp.GE_I -> if (compare(op, lhs, rhs)) 1 else 0
        AluOp.LTU_I -> if (lhs.value.toUInt() < rhs.value.toUInt()) 1 else 0
    }
    return IntValue(result)
}

fun compare(op: AluOp, lhs: IntValue, rhs: IntValue): Boolean {
    return when (op) {
        AluOp.EQ_I -> lhs.value == rhs.value
        AluOp.NE_I -> lhs.value != rhs.value
        AluOp.LT_I -> lhs.value < rhs.value
        AluOp.GT_I -> lhs.value > rhs.value
        AluOp.LE_I -> lhs.value <= rhs.value
        AluOp.GE_I -> lhs.value >= rhs.value
        else -> error("Invalid branch op $op")
    }
}

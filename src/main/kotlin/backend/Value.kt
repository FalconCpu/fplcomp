package backend

import frontend.ClassType
import frontend.SymbolField

sealed class Value() {
    fun getIntValue() = (this as IntValue).value
    fun getStringValue() = (this as StringValue).value
    fun getArrayValue() = (this as ArrayValue).value
    fun getClassValue() = (this as ClassValue)
}

class IntValue(val value: Int) : Value()  {
    override fun toString() = value.toString()
}

class StringValue(val value: String) : Value() {
    override fun toString() = "\"$value\""
}

class ArrayValue(val value: Array<Value>) : Value()

class ClassValue(val classRef: ClassType, val fields: Array<Value>) : Value()

class ClassRefValue(val classRef: ClassType) : Value() {
    override fun toString() = classRef.name
}

object UndefinedValue : Value()

fun evaluate(op: AluOp, lhs: Int, rhs: Int): Value {
    val result = when (op) {
        AluOp.ADD_I -> lhs + rhs
        AluOp.SUB_I -> lhs - rhs
        AluOp.MUL_I -> lhs * rhs
        AluOp.DIV_I -> lhs / rhs
        AluOp.MOD_I -> lhs % rhs
        AluOp.AND_I -> lhs and rhs
        AluOp.OR_I -> lhs or rhs
        AluOp.XOR_I -> lhs xor rhs
        AluOp.LSL_I -> lhs shl rhs
        AluOp.LSR_I -> lhs ushr rhs
        AluOp.ASR_I -> lhs shr rhs
        AluOp.EQ_I,
        AluOp.NE_I,
        AluOp.LT_I,
        AluOp.GT_I,
        AluOp.LE_I,
        AluOp.GE_I -> if (compare(op, lhs, rhs)) 1 else 0
        AluOp.LTU_I -> if (lhs.toUInt() < rhs.toUInt()) 1 else 0
    }
    return IntValue(result)
}

fun compare(op: AluOp, lhs: Int, rhs: Int): Boolean {
    return when (op) {
        AluOp.EQ_I -> lhs == rhs
        AluOp.NE_I -> lhs != rhs
        AluOp.LT_I -> lhs < rhs
        AluOp.GT_I -> lhs > rhs
        AluOp.LE_I -> lhs <= rhs
        AluOp.GE_I -> lhs >= rhs
        else -> error("Invalid branch op $op")
    }
}

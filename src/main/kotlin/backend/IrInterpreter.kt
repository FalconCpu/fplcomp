package backend

import frontend.SymbolGlobalVar

// This file contains a basic Interpreter for the IR code.
// It is not planned to be efficient, but it can be used to get a reference output for comparison with the
// compiler backend.

private val globalVariables = mutableMapOf<SymbolGlobalVar,Value>()
private val allRegisters = mutableMapOf<Reg, Value>()
private var localVariables = mutableMapOf<Reg, Value>()
private val programOutput = StringBuilder()

private val debug = true

fun runInterpreter(): String {
    globalVariables.clear()
    allRegisters.clear()
    allRegisters[ allMachineRegs[0] ] = IntValue(0)
    programOutput.clear()
    allFunctions.forEach{it.rebuildIndex()}
    allFunctions[0].run()
    return programOutput.toString()
}

private fun Function.run() {
    var pc = 0
    val oldLocalVariables = localVariables
    localVariables = mutableMapOf()

    while (true) {
        val instr = prog[pc]
        pc++
        when(instr) {
            is InstrAlu ->
                instr.dest.setValue( evaluate(instr.op, instr.lhs.getIntValue(), instr.rhs.getIntValue()) )

            is InstrBranch ->
                if (compare(instr.op, instr.lhs.getIntValue(), instr.rhs.getIntValue()))
                    pc = instr.target.index

            is InstrEnd -> {
                localVariables = oldLocalVariables
                return
            }

            is InstrJsr ->
                if (instr.target is StdLibFunction)
                    executeStdlibCall(instr.target)
                else
                    instr.target.run()

            is InstrJump -> {
                // if (debug) println("Jumping to ${instr.target} ${instr.target.index}")
                pc = instr.target.index
            }

            is InstrLabel -> {} // do nothing

            is InstrLea ->
                instr.dest.setValue(instr.src)

            is InstrLoad ->
                TODO()

            is InstrMov -> instr.dest.setValue(instr.src.getValue())

            is InstrStart -> {}

            is InstrStore ->
                TODO()

            is InstrLit ->
                instr.dest.setValue(IntValue(instr.value))
        }
    }
}

private fun Reg.getValue() : Value {
    return when (this) {
        is MachineReg -> allRegisters.getValue(this)
        is TempReg -> localVariables.getValue(this)
        is UserReg -> localVariables.getValue(this)
    }
}

private fun Reg.getIntValue() : IntValue {
    val value = getValue()
    check (value is IntValue)
    return value
}

private fun Reg.getArrayValue() : ArrayValue {
    val value = getValue()
    check (value is ArrayValue)
    return value
}


private fun Reg.setValue(value:Value) {
    when (this) {
        is MachineReg -> allRegisters[this] = value
        is TempReg,
        is UserReg -> localVariables[this] = value
    }
}

private fun executeStdlibCall(target: StdLibFunction) {
    val arg1 = allRegisters.getValue( allMachineRegs[1])
    val resultReg = allMachineRegs[8]

    when (target) {
        StdlibPrintBool -> programOutput.append(if ((arg1 as IntValue).value == 0) "false" else "true")
        StdlibPrintChar -> programOutput.append((arg1 as IntValue).value.toChar())
        StdlibPrintInt -> programOutput.append((arg1 as IntValue).value)
        StdlibPrintString -> programOutput.append((arg1 as StringValue).value)
        StdlibNewline -> programOutput.append("\n")
    }
}
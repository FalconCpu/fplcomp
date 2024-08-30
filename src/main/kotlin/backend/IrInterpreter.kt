package backend

import frontend.SymbolGlobalVar
import frontend.sizeSymbol

// This file contains a basic Interpreter for the IR code.
// It is not planned to be efficient, but it can be used to get a reference output for comparison with the
// compiler backend.

private val globalVariables = mutableMapOf<SymbolGlobalVar,Value>()
private val allRegisters = mutableMapOf<Reg, Value>()
private var localVariables = mutableMapOf<Reg, Value>()
private val programOutput = StringBuilder()

private val debug = false

fun runInterpreter(): String {
    globalVariables.clear()
    allRegisters.clear()
    allRegisters[ allMachineRegs[0] ] = IntValue(0)
    allRegisters[ allMachineRegs[8] ] = UndefinedValue
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
            is InstrNop -> {}

            is InstrAlu ->
                instr.dest.setValue( evaluate(instr.op, instr.lhs.getIntValue(), instr.rhs.getIntValue()) )

            is InstrAluLit ->
                instr.dest.setValue( evaluate(instr.op, instr.lhs.getIntValue(), instr.rhs) )


            is InstrBranch ->
                if (compare(instr.op, instr.lhs.getIntValue(), instr.rhs.getIntValue()))
                    pc = instr.target.index

            is InstrEnd -> {
                localVariables = oldLocalVariables
                return
            }

            is InstrCall ->
                if (instr.target is StdLibFunction)
                    executeStdlibCall(instr.target)
                else
                    instr.target.run()

            is InstrVirtCall -> {
                val instance = instr.arg.getValue() as ClassValue
                val func = instance.classRef.methods[instr.func.funcNo].function
                func.backendFunction.run()
            }




            is InstrJump -> {
                // if (debug) println("Jumping to ${instr.target} ${instr.target.index}")
                pc = instr.target.index
            }

            is InstrLabel -> {} // do nothing

            is InstrLea ->
                instr.dest.setValue(instr.src)

            is InstrLoadArrayLit -> {
                check(instr.size==4)
                val array = instr.addr.getArrayValue()
                instr.dest.setValue( array[instr.offset] )
            }

            is InstrLoadArray -> {
                check(instr.size==4)
                val array = instr.addr.getArrayValue()
                instr.dest.setValue( array[instr.offset.getIntValue()])
            }

            is InstrMov -> instr.dest.setValue(instr.src.getValue())

            is InstrStart -> {}

            is InstrStoreArrayLit -> {
                check(instr.size==4)
                val array = instr.addr.getArrayValue()
                array[instr.offset] = instr.data.getValue()
            }

            is InstrLit ->
                instr.dest.setValue(IntValue(instr.value))


            is InstrStoreArray -> {
                check(instr.size==4)
                val array = instr.addr.getArrayValue()
                val offset = instr.offset.getIntValue()
                array[offset] = instr.data.getValue()
            }

            is InstrLoadField -> {
                val addr = instr.addr.getValue()
                if (addr is ArrayValue && instr.offset== sizeSymbol)
                    instr.dest.setValue(IntValue(addr.value.size))
                else if (addr is ClassValue)
                    instr.dest.setValue( addr.fields[instr.offset.offset] )
                else
                    error("Illegal type in InstrLoadField ${addr.javaClass}")
            }

            is InstrStoreField -> {
                val addr = instr.addr.getValue()
                check(addr is ClassValue)
                addr.fields[instr.offset.offset] = instr.data.getValue()
            }

            is InstrLoadGlobal -> instr.dest.setValue( globalVariables.getValue(instr.globalVar) )
            is InstrStoreGlobal -> globalVariables[instr.globalVar] = instr.data.getValue()
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

private fun Reg.getIntValue() : Int {
    val value = getValue()
    check (value is IntValue)
    return value.value
}

private fun Reg.getArrayValue() : Array<Value> {
    val value = getValue()
    check (value is ArrayValue)
    return value.value
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

    when (target) {
        is StdlibPrintBool -> target.execute(arg1, programOutput)
        is StdlibPrintChar -> target.execute(arg1, programOutput)
        is StdlibPrintInt -> target.execute(arg1, programOutput)
        is StdlibPrintString -> target.execute(arg1, programOutput)
        is StdlibNewline -> target.execute(programOutput)
        is StdlibMallocArray -> regResult.setValue( target.execute(arg1) )
        is StdlibMallocObject -> regResult.setValue( target.execute(arg1) )
        is StdlibStrcat -> regResult.setValue( target.execute(arg1, allRegisters.getValue(allMachineRegs[2])) )
    }
}


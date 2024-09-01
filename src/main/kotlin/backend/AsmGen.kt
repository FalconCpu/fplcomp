package backend

import frontend.Symbol.Companion.UNDEFINED_OFFSET

private val sb = StringBuilder()
lateinit var currentFunction : Function

private fun Reg.checkIsReg() {
    check(index <= 31) { "Register $name is not a machine register" }
}

private fun Int.checkIsSmall() {
    check(this in -0x400..0x3ff) { "Value $this is not a small integer" }
}

private fun genPreambleTop() {
    sb.append("ld %sp, 04000000H \n")
    sb.append("jmp main()\n")
}

private fun genPostambleTop() {
    sb.append("ld %30,0\n")
    sb.append("jmp %30[0]\n")
}


private fun genPreamble(function: Function) {
    if (function.name == "<top>")
        return genPreambleTop()

    sb.append("${function.name}:\n")
    val stackFrameSize = function.stackFrameSize + (if (function.maxRegister>8) 4*(function.maxRegister - 8) else 0) +
                      if (function.makesCalls) 4 else 0
    if (stackFrameSize > 0) {
        sb.append("sub %sp, %sp, $stackFrameSize\n")
        for(index in 9..function.maxRegister)
            sb.append("stw %$index, %sp[${function.stackFrameSize + ((index - 9) * 4)}]\n")
        if (function.makesCalls)
            sb.append("stw %30, %sp[${function.stackFrameSize + 4 * (function.maxRegister - 8)}]\n")
    }
}

private fun genPostamble(function: Function) {
    if (function.name == "<top>")
        return genPostambleTop()
    val stackFrameSize = function.stackFrameSize + (if (function.maxRegister>8) 4*(function.maxRegister - 8) else 0) +
            if (function.makesCalls) 4 else 0
    if (stackFrameSize > 0) {
        for(index in 9..function.maxRegister)
            sb.append("ldw %$index, %sp[${function.stackFrameSize + ((index-9) * 4)}]\n")
        if (function.makesCalls)
            sb.append("ldw %30, %sp[${function.stackFrameSize + 4 * (function.maxRegister - 8)}]\n")
        sb.append("add %sp, %sp, $stackFrameSize\n")
    }
    sb.append("ret\n\n")
}


fun Instr.asmGen() {
    when (this) {
        is InstrAlu -> {
            dest.checkIsReg()
            lhs.checkIsReg()
            rhs.checkIsReg()
            sb.append("$op $dest, $lhs, $rhs\n")
        }

        is InstrAluLit -> {
            dest.checkIsReg()
            lhs.checkIsReg()
            rhs.checkIsSmall()
            sb.append("$op $dest, $lhs, $rhs\n")
        }

        is InstrBranch -> {
            lhs.checkIsReg()
            rhs.checkIsReg()
            // In our IR we have a branch instruction for each of the six comparisons, but the assembler
            // only has = != < >= So we have to map > and <= to < and >= by swapping the operands.
            when (op) {
                AluOp.EQ_I -> sb.append("beq $lhs, $rhs, $target\n")
                AluOp.NE_I -> sb.append("bne $lhs, $rhs, $target\n")
                AluOp.LT_I -> sb.append("blt $lhs, $rhs, $target\n")
                AluOp.GE_I -> sb.append("bge $lhs, $rhs, $target\n")
                AluOp.GT_I -> sb.append("blt $rhs, $lhs, $target\n")
                AluOp.LE_I -> sb.append("bge $rhs, $lhs, $target\n")
                else -> error("Invalid op in branch $op")
            }
        }

        is InstrCall -> {
            sb.append("jsr $target\n")
        }

        is InstrEnd -> {
            genPostamble(currentFunction)
        }

        is InstrJump -> {
            sb.append("jmp $target\n")
        }

        is InstrLabel -> {
            sb.append("$label:\n")
        }

        is InstrLea -> {
            dest.checkIsReg()
            sb.append("ld $dest, $src\n")
        }

        is InstrLit -> {
            dest.checkIsReg()
            sb.append("ld $dest, $value\n")
        }

        is InstrLoadArray -> {
            dest.checkIsReg()
            sb.append("${size.load} $dest, $addr[$offset]\n")
        }

        is InstrLoadField -> {
            dest.checkIsReg()
            sb.append("ldw $dest, $addr[${field.offset*4}]\n")
        }

        is InstrLoadGlobal -> {
            dest.checkIsReg()
            check(globalVar.offset != UNDEFINED_OFFSET)
            sb.append("ldw $dest, $regGlobal[${globalVar.offset}]\n")
        }

        is InstrMov -> {
            dest.checkIsReg()
            src.checkIsReg()
            sb.append("ld $dest, $src\n")
        }

        is InstrNop -> {
            sb.append("nop\n")
        }

        is InstrStart -> {
            genPreamble(currentFunction)
        }

        is InstrStoreArray -> {
            addr.checkIsReg()
            sb.append("${size.store} $data, $addr[$offset]\n")
        }

        is InstrStoreField -> {
            addr.checkIsReg()
            sb.append("${size.store} $data, $addr[${field.offset*4}]\n")
        }

        is InstrStoreGlobal -> {
            data.checkIsReg()
            check(globalVar.offset != UNDEFINED_OFFSET)
            sb.append("stw $data, $regGlobal[${globalVar.offset}]\n")
        }

        is InstrVirtCall -> {
            val methodId = this.target.methodId
            sb.append("ldw %30, %1[-4]\n")    // Get the function's Vtable
            sb.append("ldw %30, %30[${methodId*4}]\n") // Get the function's address
            sb.append("jmp %30, %30[0]\n")
        }

    }
}

fun Function.asmGen() {
    currentFunction = this
    for(instr in prog)
        instr.asmGen()
}

fun asmGen(): String {
    sb.clear()
    for(func in allFunctions) {
        func.asmGen()
    }
    return sb.toString()
}
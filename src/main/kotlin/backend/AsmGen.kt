package backend

import com.sun.org.apache.xpath.internal.operations.Bool
import frontend.ArrayType
import frontend.ClassType
import frontend.PointerType
import frontend.Symbol.Companion.UNDEFINED_OFFSET
import frontend.allArrayTypes
import frontend.allClassTypes
import frontend.allConstantArrays
import frontend.allPointerTypes

private val sb = StringBuilder()
lateinit var currentFunction : Function

private var org = "0FFFF0000H"

private fun Reg.checkIsReg() {
    check(index <= 31) { "Register $name is not a machine register" }
}

private fun Int.checkIsSmall() {
    check(this in -0x400..0x3ff) { "Value $this is not a small integer" }
}

private fun genPreamble(function: Function) {
    if (function.name == "<top>") {
        sb.append("org $org\n")      // Value for emulator
        sb.append("premain:\n")
        sb.append("ld %sp, 4000000H\n")
        sb.append("jsr initializeMemory()\n")
        return
    }

    sb.append("${function.name}:\n")

    val stackFrameSize = function.stackFrameSize + (if (function.maxRegister>8) 4*(function.maxRegister - 8) else 0) +
                      if (function.makesCalls) 4 else 0
    if (stackFrameSize > 0) {
        sb.append("sub %sp, %sp, $stackFrameSize\n")
        var offset = function.stackFrameSize
        for(index in 9..function.maxRegister) {
            sb.append("stw %$index, %sp[$offset]\n")
            offset += 4
        }
        if (function.makesCalls)
            sb.append("stw %30, %sp[$offset]\n")
    }
}

private fun genPostamble(function: Function) {
    if (function.name == "<top>") {
        sb.append("ld %1, 0\n")
        sb.append("ld %2, 0\n")
        sb.append("jsr fatal(Int,Int)\n\n")
        return
    }

    val stackFrameSize = function.stackFrameSize + (if (function.maxRegister>8) 4*(function.maxRegister - 8) else 0) +
            if (function.makesCalls) 4 else 0
    if (stackFrameSize > 0) {
        var offset = function.stackFrameSize
        for(index in 9..function.maxRegister) {
            sb.append("ldw %$index, %sp[$offset]\n")
            offset += 4
        }
        if (function.makesCalls)
            sb.append("ldw %30, %sp[$offset]\n")
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
            when(op) {
                AluOp.EQ_I -> sb.append("xor $dest, $lhs, $rhs\ncltu $dest, $dest, 1\n")
                AluOp.NE_I -> sb.append("xor $dest, $lhs, $rhs\ncltu $dest, 0, $dest\n")
                AluOp.LT_I -> sb.append("clt $dest, $lhs, $rhs\n")
                AluOp.GT_I -> sb.append("clt $dest, $rhs, $lhs\n")
                AluOp.LE_I -> sb.append("clt $dest, $rhs, $lhs\nxor $dest, $dest, 1\n")
                AluOp.GE_I -> sb.append("clt $dest, $lhs, $rhs\nxor $dest, $dest, 1\n")
                else -> sb.append("$op $dest, $lhs, $rhs\n")
            }
        }

        is InstrAluLit -> {
            dest.checkIsReg()
            lhs.checkIsReg()
            rhs.checkIsSmall()
            when(op) {
                AluOp.EQ_I -> sb.append("xor $dest, $lhs, $rhs\ncltu $dest, $dest, 1\n")
                AluOp.NE_I -> sb.append("xor $dest, $lhs, $rhs\ncltu $dest, 0, $dest\n")
                AluOp.LT_I -> sb.append("clt $dest, $lhs, $rhs\n")
                AluOp.GT_I -> sb.append("clt $dest, $lhs, ${rhs+1}\nxor $dest, $dest, 1\n")
                AluOp.LE_I -> sb.append("clt $dest, $lhs, ${rhs+1}\n")
                AluOp.GE_I -> sb.append("clt $dest, $lhs, $rhs\nxor $dest, $dest, 1\n")
                else -> sb.append("$op $dest, $lhs, $rhs\n")
            }
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

        is InstrLeaType -> {
            dest.checkIsReg()
            sb.append("ld $dest, $src|class\n")
        }

        is InstrLeaArrayImage -> {
            dest.checkIsReg()
            sb.append("ld $dest, $src\n")
        }

        is InstrLeaString -> {
            dest.checkIsReg()
            sb.append("ld $dest, ${src.escape()}\n")
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
            sb.append("ldw $dest, $regGlobal[${globalVar.offset*4}]\n")
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
            sb.append("stw $data, $regGlobal[${globalVar.offset*4}]\n")
        }

        is InstrVirtCall -> {
            val methodId = this.target.methodId
            sb.append("ldw %30, %1[-4]\n")    // Get the function's Vtable
            sb.append("ldw %30, %30[${methodId*4}]\n") // Get the function's address
            sb.append("jmp %30, %30[0]\n")
        }

    }
}

private fun Function.asmGen() {
    if (isExternal)
        return      // Don't generate code for external functions

    currentFunction = this
    for(instr in prog)
        instr.asmGen()
}

private fun ClassType.asmGen() {
    sb.append("$name|class:\n")
    sb.append("dcw \"$name\"\n")             // Class name
    sb.append("dcw ${this.numFields*4}\n")   // Size of the class's fields
    for(method in methods)                   // Vtable
        sb.append("dcw ${method.name}\n")
    sb.append("\n")
}

private fun ArrayType.asmGen() {
    sb.append("$name|class:\n")
    sb.append("dcw \"$name\"\n")             // Class name
    sb.append("dcw 0\n")                     // Size of the class's fields
    sb.append("\n")
}

private fun PointerType.asmGen() {
    sb.append("$name|class:\n")
    sb.append("dcw \"$name\"\n")             // Class name
    sb.append("dcw 0\n")                     // Size of the class's fields
    sb.append("\n")
}




fun asmGen(forFpga: Boolean): String {
    sb.clear()
    org = if (forFpga) "0FFFF8000H" else "0FFFF0000H"

    for(func in allFunctions)
        func.asmGen()

    for (cls in allClassTypes)
        cls.asmGen()

    for (cls in allArrayTypes)
        cls.asmGen()

    for (cls in allPointerTypes)
        cls.asmGen()

    sb.append("String|class:\n")
    sb.append("dcw \"String\"\n")
    sb.append("dcw 0\n")


    for (ary in allConstantArrays)
        ary.asmGen(sb)

    return sb.toString()
}
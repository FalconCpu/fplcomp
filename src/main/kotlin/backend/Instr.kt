package backend

import frontend.SymbolField
import frontend.SymbolFunctionName
import frontend.SymbolGlobalVar

sealed class Instr {
    var index = 0

    fun getDef() = when (this) {
        is InstrNop -> null
        is InstrAlu -> dest
        is InstrAluLit -> dest
        is InstrBranch -> null
        is InstrCall -> null
        is InstrEnd -> null
        is InstrJump -> null
        is InstrLabel -> null
        is InstrLea -> dest
        is InstrLit -> dest
        is InstrLoadArray -> dest
        is InstrLoadArrayLit -> dest
        is InstrLoadField -> dest
        is InstrLoadGlobal -> dest
        is InstrMov -> dest
        is InstrStart -> null
        is InstrStoreArray -> null
        is InstrStoreArrayLit -> null
        is InstrStoreField -> null
        is InstrStoreGlobal -> null
        is InstrVirtCall -> null
    }

    fun getUses() = when (this) {
        is InstrNop -> emptyList()
        is InstrAlu -> listOf(lhs, rhs)
        is InstrAluLit -> listOf(lhs)
        is InstrBranch -> listOf(lhs, rhs)
        is InstrCall -> emptyList()
        is InstrEnd -> listOf(regResult)
        is InstrJump -> emptyList()
        is InstrLabel -> emptyList()
        is InstrLea -> emptyList()
        is InstrLit -> emptyList()
        is InstrLoadArray -> listOf(addr, offset)
        is InstrLoadArrayLit -> listOf(addr)
        is InstrLoadField -> listOf(addr)
        is InstrLoadGlobal -> emptyList()
        is InstrMov -> listOf(src)
        is InstrStart -> emptyList()
        is InstrStoreArray -> listOf(addr, data, offset)
        is InstrStoreArrayLit -> listOf(addr, data)
        is InstrStoreField -> listOf(addr, data)
        is InstrStoreGlobal -> listOf(data)
        is InstrVirtCall -> emptyList()
    }
}

class InstrNop() : Instr() {
    override fun toString() = "nop"
}


class InstrMov(val dest: Reg, val src:Reg) : Instr() {
    override fun toString() = "mov $dest, $src"
}

class InstrAlu(val dest:Reg, val op: AluOp, val lhs:Reg, val rhs:Reg)  : Instr() {
    override fun toString() = "$op $dest, $lhs, $rhs"
}

class InstrAluLit(val dest:Reg, val op: AluOp, val lhs:Reg, val rhs:Int)  : Instr() {
    init {
        check(rhs in -0x400..0x3ff)
    }
    override fun toString() = "$op $dest, $lhs, $rhs"
}

class InstrLit(val dest:Reg, val value: Int) : Instr() {
    override fun toString() = "mov $dest, $value"
}

class InstrLabel(val label: Label) : Instr() {
    override fun toString() = "$label:"
}

class InstrJump(val target: Label) : Instr() {
    override fun toString() = "jmp $target"
}

class InstrBranch(val op: AluOp, val lhs:Reg, val rhs:Reg, val target: Label) : Instr() {
    val opx = when(op) {
        AluOp.EQ_I -> "beq"
        AluOp.NE_I -> "bne"
        AluOp.LT_I -> "blt"
        AluOp.LE_I -> "ble"
        AluOp.GT_I -> "bgt"
        AluOp.GE_I -> "bge"
        else -> error("Invalid op in branch $op")
    }
    override fun toString() = "$opx, $lhs, $rhs, $target"
}

class InstrCall(val target: Function) : Instr() {
    override fun toString() = "call $target"
}

class InstrVirtCall(val arg: Reg, val func: SymbolFunctionName) : Instr() {
    override fun toString() = "virtcall $arg, $func"
}


class InstrStart() : Instr() {
    override fun toString() = "start"
}

class InstrEnd() : Instr() {
    override fun toString() = "end"
}

class InstrLoadArrayLit(val size:Int, val dest: Reg, val addr: Reg, val offset:Int) : Instr() {
    override fun toString() = "ldw $dest, $addr[$offset]"
}

class InstrStoreArrayLit(val size:Int, val data: Reg, val addr: Reg, val offset:Int) : Instr() {
    override fun toString() = "stw $data, $addr[$offset]"
}

class InstrLoadArray(val size:Int, val dest: Reg, val addr: Reg, val offset:Reg) : Instr() {
    override fun toString() = "ldw $dest, $addr[$offset]"
}

class InstrStoreArray(val size:Int, val data: Reg, val addr: Reg, val offset:Reg) : Instr() {
    override fun toString() = "stw $data, $addr[$offset]"
}

class InstrLoadField(val size:Int, val dest: Reg, val addr: Reg, val offset:SymbolField) : Instr() {
    override fun toString() = "ldw $dest, $addr->$offset"
}

class InstrStoreField(val size:Int, val data: Reg, val addr: Reg, val offset:SymbolField) : Instr() {
    override fun toString() = "stw $data, $addr->$offset"
}

class InstrLoadGlobal(val size:Int, val dest: Reg, val globalVar: SymbolGlobalVar) : Instr() {
    override fun toString() = "ldw $dest, GLOBAL->$globalVar"
}

class InstrStoreGlobal(val  size:Int, val data: Reg, val globalVar: SymbolGlobalVar) : Instr() {
    override fun toString() = "stw $data, GLOBAL->$globalVar"
}

class InstrLea(val dest:Reg, val src:Value) : Instr() {
    override fun toString() = "lea $dest, $src"
}

enum class AluOp(val text:String) {
    ADD_I ("add"),
    SUB_I ("sub"),
    MUL_I ("mul"),
    DIV_I ("div"),
    MOD_I ("mod"),
    AND_I ("and"),
    OR_I ("or"),
    XOR_I ("xor"),
    LSL_I ("lsl"),
    LSR_I ("lsr"),
    ASR_I ("asr"),
    EQ_I ("ceq"),
    NE_I ("cne"),
    LT_I ("clt"),
    GT_I ("cgt"),
    LE_I ("cle"),
    GE_I ("cgt"),
    LTU_I ("cltu");

    override fun toString() = text
}

enum class MemSize(val text: String) {
    BYTE("byte"),
    HALF("halfword"),
    WORD("word"),
    DWORD("dword");

    override fun toString() = text

}


class Label (val name:String) {
    var index = 0
    val uses = mutableSetOf<Instr>()

    override fun toString() = name
}

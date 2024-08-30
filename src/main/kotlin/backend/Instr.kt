package backend

import frontend.SymbolField
import frontend.SymbolFunctionName
import frontend.SymbolGlobalVar

sealed class Instr {
    var index = 0
}

class InstrMov(val dest: Reg, val src:Reg) : Instr() {
    override fun toString() = "$dest = $src"
}

class InstrAlu(val dest:Reg, val op: AluOp, val lhs:Reg, val rhs:Reg)  : Instr() {
    override fun toString() = "$dest = $lhs $op $rhs"
}

class InstrAluLit(val dest:Reg, val op: AluOp, val lhs:Reg, val rhs:Int)  : Instr() {
    init {
        check(rhs in -0x400..0x3ff)
    }
    override fun toString() = "$dest = $lhs $op $rhs"
}


class InstrLit(val dest:Reg, val value: Int) : Instr() {
    override fun toString() = "$dest = $value"
}

class InstrLabel(val label: Label) : Instr() {
    override fun toString() = "$label:"
}

class InstrJump(val target: Label) : Instr() {
    override fun toString() = "jmp $target"
}

class InstrBranch(val op: AluOp, val lhs:Reg, val rhs:Reg, val target: Label) : Instr() {
    override fun toString() = "if $lhs $op $rhs jmp $target"
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
    override fun toString() = "$dest = $addr[$offset]"
}

class InstrStoreArrayLit(val size:Int, val data: Reg, val addr: Reg, val offset:Int) : Instr() {
    override fun toString() = "$addr[$offset] = $data"
}

class InstrLoadArray(val size:Int, val dest: Reg, val addr: Reg, val offset:Reg) : Instr() {
    override fun toString() = "$dest = $addr[$offset]"
}

class InstrStoreArray(val size:Int, val data: Reg, val addr: Reg, val offset:Reg) : Instr() {
    override fun toString() = "$addr[$offset] = $data"
}

class InstrLoadField(val size:Int, val dest: Reg, val addr: Reg, val offset:SymbolField) : Instr() {
    override fun toString() = "$dest = $addr->$offset"
}

class InstrStoreField(val size:Int, val data: Reg, val addr: Reg, val offset:SymbolField) : Instr() {
    override fun toString() = "$addr->$offset = $data"
}

class InstrLoadGlobal(val size:Int, val dest: Reg, val globalVar: SymbolGlobalVar) : Instr() {
    override fun toString() = "$dest = GLOBAL[$globalVar]"
}

class InstrStoreGlobal(val  size:Int, val data: Reg, val globalVar: SymbolGlobalVar) : Instr() {
    override fun toString() = "GLOBAL[$globalVar] = $data"
}



class InstrLea(val dest:Reg, val src:Value) : Instr() {
    override fun toString() = "$dest = ADDR($src)"
}

enum class AluOp(val text:String) {
    ADD_I ("+"),
    SUB_I ("-"),
    MUL_I ("*"),
    DIV_I ("/"),
    MOD_I ("%"),
    AND_I ("&"),
    OR_I ("|"),
    XOR_I ("^"),
    LSL_I ("<<"),
    LSR_I (">>>"),
    ASR_I (">>"),
    EQ_I ("=="),
    NE_I ("!="),
    LT_I ("<"),
    GT_I (">"),
    LE_I ("<="),
    GE_I (">="),
    LTU_I ("<#");

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

    override fun toString() = name
}

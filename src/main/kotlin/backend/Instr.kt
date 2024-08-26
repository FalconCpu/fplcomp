package backend
import frontend.Symbol

sealed class Instr {
    var index = 0
}

class InstrMov(val dest: Reg, val src:Reg) : Instr() {
    override fun toString() = "$dest = $src"
}

class InstrAlu(val dest:Reg, val op: AluOp, val lhs:Reg, val rhs:Reg)  : Instr() {
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

class InstrJsr(val target: Function) : Instr() {
    override fun toString() = "call $target"
}

class InstrStart() : Instr() {
    override fun toString() = "start"
}

class InstrEnd() : Instr() {
    override fun toString() = "end"
}

class InstrLoad(val size:MemSize, val dest: Reg, val addr: Reg, val offset:Symbol) : Instr() {
    override fun toString() = "$dest = $addr[$offset]"
}

class InstrStore(val size:Int, val data: Reg, val addr: Reg, val offset:Symbol) : Instr() {
    override fun toString() = "ST$size $data, $addr[$offset]"
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

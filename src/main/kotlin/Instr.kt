package falcon

sealed class Instr {
    var index = 0
}

class InstrMov(val dest:Symbol, val src:Symbol) : Instr() {
    override fun toString() = "MOV  $dest, $src"
}

class InstrAlu(val dest:Symbol, val op:AluOp, val lhs:Symbol, val rhs:Symbol)  : Instr() {
    override fun toString() = "$op  $dest, $lhs, $rhs"
}

class InstrLabel(val label:Label) : Instr() {
    override fun toString() = "$label:"
}

class InstrJump(val target: Label) : Instr() {
    override fun toString() = "JMP  $target"
}

class InstrBranch(val op: AluOp, val lhs:Symbol, val rhs:Symbol, val target:Label) : Instr() {
    override fun toString() = "B$op   $lhs, $rhs, $target"
}

class InstrJsr(val target: Function) : Instr() {
    override fun toString() = "JSR  $target"
}

class InstrStart() : Instr() {
    override fun toString() = "START"
}

class InstrEnd() : Instr() {
    override fun toString() = "END"
}

class InstrLoad(val size:Int, val dest: Symbol, val addr: Symbol, val offset:Symbol) : Instr() {
    override fun toString() = "LD$size $dest, $addr[$offset]"
}

class InstrStore(val size:Int, val data: Symbol, val addr: Symbol, val offset:Symbol) : Instr() {
    override fun toString() = "ST$size $data, $addr[$offset]"
}

class InstrLea(val dest:Symbol, val src:Symbol) : Instr() {
    override fun toString() = "LEA  $dest, $src"
}


enum class AluOp {
    ADD_I,
    SUB_I,
    MUL_I,
    DIV_I,
    MOD_I,
    AND_I,
    OR_I,
    XOR_I,
    SHL_I,
    SHR_I,
    EQ_I,
    NE_I,
    LT_I,
    GT_I,
    LE_I,
    GE_I,
    LTU_I,
}

class Label (val name:String) {
    var index = 0

    override fun toString() = name
}

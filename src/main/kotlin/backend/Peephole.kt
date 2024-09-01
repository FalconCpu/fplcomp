package backend

class Peephole(val cf: Function) {
    var madeChange = false
    val debug = false

    private fun Instr.replace(new:Instr) {
        if (cf.prog[index] != new) {
            if (debug)
                println("Replacing $index with $new")
            cf.prog[index] = new
            madeChange = true
        }
    }

    private fun Instr.changeToNop() {
        if (cf.prog[index] !is InstrNop) {
            if (debug)
                println("Replacing $index with NOP")
            cf.prog[index] = InstrNop()
            madeChange = true
        }
    }

    fun Reg.regAsSmallConst() : Int? {
        if (this !is TempReg) return null
        if (this.defs.size != 1) return null
        val source = this.defs.first()
        if (source !is InstrLit) return null
        if (source.value < -0x400 || source.value > 0x3ff) return null
        return source.value
    }

    fun Reg.regAsConst() : Int? {
        if (this !is TempReg) return null
        if (this.defs.size != 1) return null
        val source = this.defs.first()
        if (source !is InstrLit) return null
        return source.value
    }

    fun Reg.isLiteralZero() : Boolean {
        if (this !is TempReg) return false
        if (this.defs.size != 1) return false
        val source = this.defs.first()
        if (source !is InstrLit) return false
        return source.value == 0
    }



    fun InstrAlu.peephole() {
        // Look to see if one of the operands is a constant
        val rhsConst = rhs.regAsSmallConst()
        if (rhsConst != null)
            return replace( InstrAluLit(dest, op, lhs, rhsConst))
    }

    fun InstrMov.peephole() {
        // Look to see if one of the operands is a constant
        val rhsConst = src.regAsConst()
        if (rhsConst != null)
            return replace(InstrLit(dest, rhsConst))

        // If the source and dest are the same, then remove
        if (dest == src) return changeToNop()
    }

    fun InstrBranch.peephole() {
        // If the jump just goes to the next instruction, then remove
        if (target.index == index+1 ) return changeToNop()

        if (lhs.isLiteralZero())
            return replace(InstrBranch(op, regZero, rhs, target))

        if (rhs.isLiteralZero())
            return replace(InstrBranch(op, lhs, regZero, target))
    }


    fun InstrJump.peephole() {
        // If the jump just goes to the next instruction, then remove
        if (target.index == index+1 ) return changeToNop()
        if (target.index == index+2 && cf.prog[index+1] is InstrLabel ) return changeToNop()


    }

    fun InstrLabel.peephole() {
        // If the jump just goes to the next instruction, then remove
        if (label.uses.isEmpty()) return changeToNop()
    }

    fun peepholePass() {
        for(instr in cf.prog) {

            // If an instruction writes to a register, but that register is never used, then remove
            // the instruction.  Don't do this for registers 0-8, since these are used for passing arguments to functions
            val dest = instr.getDef()
            if (dest!=null && dest.uses.isEmpty() && dest.index>8) {
                instr.changeToNop()
                continue
            }


            when (instr) {
                is InstrAlu -> instr.peephole()
                is InstrMov -> instr.peephole()
                is InstrJump -> instr.peephole()
                is InstrLabel -> instr.peephole()
                is InstrBranch -> instr.peephole()
                else -> {}
            }
        }
    }

    fun run() {
        do {
            madeChange = false
            if (debug)
                cf.dumpWithIndex()
            cf.rebuildIndex()
            peepholePass()
        } while (madeChange)
    }
}
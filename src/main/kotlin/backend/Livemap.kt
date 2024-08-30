package backend

import java.util.BitSet

class Livemap(private val cf: Function) {
    private val numRows = cf.prog.size
    private val numCols = cf.regs.size

    val live = Array(numRows){BitSet(numCols)}
    private val kill = Array(numRows){BitSet(numCols)}

    private fun gen() {
        for(instr in cf.prog) {
            val def = instr.getDef()
            if (def!=null)
                kill[instr.index][def.index] = true

            val use = instr.getUses()
            for (use in use)
                live[instr.index][use.index] = true

            if (instr is InstrCall || instr is InstrVirtCall)
                for (dest in 1..8)
                    kill[instr.index][dest] = true
        }
    }

    private fun propagate() {
        var madeChanges : Boolean
        do {
            madeChanges = false
            for (instr in cf.prog.asReversed()) {
                when (instr) {
                    is InstrEnd -> {}
                    is InstrJump -> {
                        val count = live[instr.index].cardinality()
                        live[instr.index].or(live[instr.target.index])
                        if (live[instr.index].cardinality() > count)
                            madeChanges=true
                    }

                    is InstrBranch -> {
                        val count = live[instr.index].cardinality()
                        live[instr.index].or(live[instr.target.index])
                        live[instr.index].or(live[instr.index + 1])
                        if (live[instr.index].cardinality() > count)
                            madeChanges=true
                    }

                    else -> {
                        // Find the bits live at next instruction that are not killed by this one
                        val x = live[instr.index + 1].clone() as BitSet
                        x.andNot(kill[instr.index])
                        live[instr.index].or(x)
                    }
                }
            }
        } while(madeChanges)
    }

    fun dump() {

        for(y in 0..4) {
            print(" ".repeat(30))
            for (x in 0..<numCols) {
                print(cf.regs[x].name.padStart(5)[y])
                if (x % 8 == 7)
                    print(' ')
            }
            print("\n")
        }

        for(y in 0..<numRows) {
            print("%-30s".format(cf.prog[y].toString()))
            for (x in 0..<numCols) {
                val l = live[y][x]
                val k = kill[y][x]
                val c = if (l && k) 'B' else if (l) 'X' else if (k) 'K' else '.'
                print(c)
                if (x%8==7)
                    print(' ')
            }
            print("\n")
        }
    }

    init {
        cf.rebuildIndex()
        gen()
        propagate()
    }
}
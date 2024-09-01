package backend

class RegisterAllocator(private val cf: Function, private val livemap: Livemap) {

    // reg numbers  0..31 represent the CPU registers,
    // CPU register number 0, 29,30 and 31 have dedicated uses. leaving numbers 1..28 for allocation
    // reg numbers 32 and up are the user variables that need to be assigned to cpu registers.
    // Register numbers 1..8 potentially get clobbered by function calls
    // Register number -1 is used to indicate currently unallocated
    companion object {
        val all_cpu_regs = 0..31
        private val cpu_regs = 1..28
        private const val UNALLOCATED = -1
        private val caller_save_regs = 1..8
    }
    private val num_vars = cf.regs.size
    private val user_vars = 32..<num_vars

    // Array of which registers are allocated to each variable
    private val alloc = Array(num_vars){ if (it<=31) it else UNALLOCATED}

    // Array of which Args interfere with each arg
    private val interfere = Array(num_vars){mutableSetOf<Int>() }

    // List of MOV statements in the prog, where both operands are variables
    private val movStatements = cf.prog.filterIsInstance<InstrMov>() // .filter{it.src.isVar() && it.dest.isVar()}

    private val debug = false


    /**
     * Build a map listing every Arg that interferes with an arg
     */
    private fun buildInterfere() {
        for (instr in cf.prog) {
            // Args written by an instruction interfere with everything live at that point
            // (Except for a MOV statement - no interference is created between its Dest and Src)
            val def = instr.getDef()
            if (def != null)
                for (liveIndex in livemap.live[instr.index + 1].stream()) {  // Get the live set for the next instruction
                    if (liveIndex != def.index && !(instr is InstrMov && liveIndex == instr.src.index)) {
                        interfere[def.index] += liveIndex
                        interfere[liveIndex] += def.index
                    }
                }

            // A call statement could potentially clobber registers %1-%8, so mark those
            if (instr is InstrCall || instr is InstrVirtCall) {
                for (liveIndex in livemap.live[instr.index + 1].stream()) {
                    for (dest in caller_save_regs)
                        if (liveIndex != dest) {
                            interfere[dest] += liveIndex
                            interfere[liveIndex] += dest
                        }
                }
            }
        }
    }

    private fun dumpInterfere() {
        println("Interfere Graph:")
        for(index in interfere.indices)
            if (interfere[index].isNotEmpty())
                println("${cf.regs[index]} = ${interfere[index].joinToString { cf.regs[it].name }}")
    }

    /**
     * Assign variable 'v' to register 'r'
     */

    private fun assign(v:Int, r:Int) {
        if (debug)
            println("Assigning ${cf.regs[v]} to ${cf.regs[r]}")
        assert(r in all_cpu_regs)
        assert(v in user_vars)
        assert(! interfere[r].contains(v))

        alloc[v] = r
        interfere[r] += interfere[v]
        if (r>cf.maxRegister && r<=28)
            cf.maxRegister = r
    }

    private fun lookForCoalesce() {
        do {
            var again = false
            for (mov in movStatements) {
                val a = mov.src.index
                val d = mov.dest.index
                if (alloc[a] == UNALLOCATED && alloc[d] != UNALLOCATED && (a !in interfere[alloc[d]])) {
                    assign(a, alloc[d])
                    again = true
                }
                if (alloc[d] == UNALLOCATED && alloc[a] != UNALLOCATED && (d !in interfere[alloc[a]])) {
                    assign(d, alloc[a])
                    again = true
                }
            }
        } while (again)
    }

    /**
     * Find a register which does not have an interference with 'v'
     */
    private fun findAssignFor(v:Int) : Int{
        for (r in cpu_regs) {
            if (v !in interfere[r])
                return r
        }
        error("Unable to find a register for ${cf.regs[v]}")
    }

    private fun replace(a: Reg) = if (a.isVar()) cf.regs[alloc[a.index]] else a

    private fun Instr.replaceVars() : Instr {
        val new = when(this) {
            is InstrAlu -> InstrAlu(replace(dest), op, replace(lhs), replace(rhs))
            is InstrAluLit -> InstrAluLit(replace(dest), op, replace(lhs), rhs)
            is InstrBranch -> InstrBranch(op, replace(lhs), replace(rhs), target)
            is InstrCall -> this
            is InstrVirtCall-> this
            is InstrEnd -> this
            is InstrJump -> this
            is InstrLabel -> this
            is InstrLea -> InstrLea(replace(dest), src)
            is InstrLoadArray -> InstrLoadArray(size, replace(dest), replace(addr), offset)
            is InstrLoadGlobal -> InstrLoadGlobal(size, replace(dest), globalVar)
            is InstrLoadField -> InstrLoadField(size, replace(dest), replace(addr), field)
            is InstrMov -> InstrMov(replace(dest), replace(src))
            is InstrStart -> this
            is InstrStoreArray -> InstrStoreArray(size, replace(data), replace(addr), offset)
            is InstrStoreGlobal -> InstrStoreGlobal(size, replace(data), globalVar)
            is InstrStoreField -> InstrStoreField(size, replace(data), replace(addr), field)
            is InstrLit -> InstrLit(replace(dest), value)
            is InstrNop -> this
        }
        new.index = index
        return new
    }

    fun run() {
        if (debug)
            livemap.dump()
        buildInterfere()
        if (debug)
            dumpInterfere()

        // Perform the allocation starting with the most difficult vars
        val vars = user_vars.sortedByDescending { interfere[it].size }
        lookForCoalesce()

        for(v in vars) {
            if (alloc[v] == UNALLOCATED) {
                val r = findAssignFor(v)
                assign(v, r)
                lookForCoalesce()
            }
        }

        for(index in cf.prog.indices)
            cf.prog[index] = cf.prog[index].replaceVars()
    }
}
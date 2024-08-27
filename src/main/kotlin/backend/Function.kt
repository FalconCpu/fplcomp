package backend

import frontend.SymbolField
import frontend.currentFunction

val allFunctions = mutableListOf<Function>()

open class Function(val name:String, isStdLib:Boolean=false) {
    init {
        if (!isStdLib)
            allFunctions.add(this)
    }

    private val vars = allMachineRegs.toMutableList<Reg>()
    val prog = mutableListOf<Instr>()
    private val symbolMap = mutableMapOf<frontend.Symbol, Reg>()
    private val labels = mutableListOf<Label>()
    val endLabel = newLabel()

    override fun toString() = name

    fun add(instr: Instr) {
        prog.add(instr)
    }

    fun newLabel() : Label {
        val label = Label("@${labels.size}")
        labels.add(label)
        return label
    }

    private var numTemps = 0
    private fun newTemp(): Reg {
        val reg = TempReg("t${numTemps++}")
        vars.add(reg)
        return reg
    }

    fun dump(sb: StringBuilder) {
        sb.append("Function $name\n")
        for (instr in prog)
            sb.append("$instr\n")
        sb.append("\n")
    }

    fun getReg(symbol: frontend.Symbol): Reg{
        return symbolMap.getOrPut(symbol) {
            val reg = UserReg(symbol.name)
            vars.add(reg)
            reg
        }
    }

    fun instrMove(dst: Reg, src: Reg) {
        add(InstrMov(dst, src))
    }

    fun instrAlu(op: AluOp, lhs: Reg, rhs: Reg) : Reg {
        val ret = newTemp()
        add(InstrAlu(ret, op, lhs, rhs))
        return ret
    }

    fun instrAlu(op: AluOp, lhs: Reg, rhs: Int) : Reg {
        val ret = newTemp()
        add(InstrAluLit(ret, op, lhs, rhs))
        return ret
    }


    fun instrInt(value: Int): Reg {
        val ret = newTemp()
        add(InstrLit(ret, value))
        return ret
    }

    fun instrLea(value: Value): Reg {
        val ret = newTemp()
        add(InstrLea(ret, value))
        return ret
    }

    fun instrJump(target: Label) {
        add(InstrJump(target))
    }

    fun instrCall(target: Function) : Reg {
        add(InstrJsr(target))
        val ret = newTemp()
        add(InstrMov(ret, regResult))
        return ret
    }


    fun instrBranch(op: AluOp, lhs: Reg, rhs: Reg, target: Label) {
        add(InstrBranch(op, lhs, rhs, target))
    }

    fun instrLabel(label: Label) {
        add(InstrLabel(label))
    }

    fun instrStore(size:Int, data:Reg, addr:Reg, offset: Int) {
        add(InstrStoreArrayLit(size, data, addr, offset))
    }

    fun instrLoad(size:Int, addr:Reg, offset: Int) : Reg {
        val ret = newTemp()
        add(InstrLoadArrayLit(size, ret, addr, offset))
        return ret
    }

    fun instrStore(size:Int, data:Reg, addr:Reg, offset: Reg) {
        add(InstrStoreArray(size, data, addr, offset))
    }

    fun instrLoad(size:Int, addr:Reg, offset: Reg) : Reg {
        val ret = newTemp()
        add(InstrLoadArray(size, ret, addr, offset))
        return ret
    }

    fun instrStore(size:Int, data:Reg, addr:Reg, offset: SymbolField) {
        add(InstrStoreField(size, data, addr, offset))
    }

    fun instrLoad(size:Int, addr:Reg, offset: SymbolField) : Reg {
        val ret = newTemp()
        add(InstrLoadField(size, ret, addr, offset))
        return ret
    }



    fun rebuildIndex() {
        for((index, instr) in prog.withIndex())
            if (instr is InstrLabel)
                instr.label.index = index
    }
}

fun dumpAllFunctions(): String {
    val sb = StringBuilder()
    for (func in allFunctions)
        func.dump(sb)
    return sb.toString()
}

fun initialize() {
    // Access some global variables - to get Kotlin to initialize them before we start
    currentFunction
    frontend.predefinedSymbols

    allFunctions.clear()
}


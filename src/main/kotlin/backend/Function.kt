package backend

import frontend.SymbolField
import frontend.SymbolGlobalVar
import frontend.TcFunction
import frontend.currentFunction

open class Function(val name:String, isStdLib:Boolean=false) {
    init {
        if (!isStdLib)
            allFunctions.add(this)
    }

    val regs = allMachineRegs.toMutableList<Reg>()
    val prog = mutableListOf<Instr>()
    private val symbolMap = mutableMapOf<frontend.Symbol, Reg>()
    private val labels = mutableListOf<Label>()
    val endLabel = newLabel()
    var thisReg : Reg? = null

    // Values to be filled in by the backend
    var maxRegister = 0    // The highest register number used



    override fun toString() = name

    fun add(instr: Instr) {
        prog.add(instr)
    }

    fun getThis() : Reg {
        return thisReg ?: error("Attempt to access 'this' for non member function")
    }

    fun newLabel() : Label {
        val label = Label("@${labels.size}")
        labels.add(label)
        return label
    }

    private var numTemps = 0
    fun newTemp(): Reg {
        val reg = TempReg("t${numTemps++}")
        regs.add(reg)
        return reg
    }

    fun dump(sb: StringBuilder) {
        sb.append("Function $name\n")
        for (instr in prog)
            sb.append("$instr\n")
        sb.append("\n")
    }

    fun dumpWithIndex() {
        for (instr in prog)
            println("%3d %s".format(prog.indexOf(instr), instr.toString()))
        println()
    }


    fun getReg(symbol: frontend.Symbol): Reg{
        return symbolMap.getOrPut(symbol) {
            val reg = UserReg(symbol.name)
            regs.add(reg)
            reg
        }
    }

    fun instrMove(dst: Reg, src: Reg) {
        add(InstrMov(dst, src))
    }

    fun instrMove(dst: Reg, src: Int) {
        add(InstrLit(dst, src))
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

    fun instrLea(dest:Reg, value: Value) {
        add(InstrLea(dest, value))
    }

    fun instrJump(target: Label) {
        add(InstrJump(target))
    }

    fun instrCall(target: Function) : Reg {
        add(InstrCall(target))
        val ret = newTemp()
        add(InstrMov(ret, regResult))
        return ret
    }

    fun instrVirtCall(instance:Reg, target: TcFunction) : Reg {
        add(InstrVirtCall(instance, target))
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

    fun instrLoad(size:Int, addr:Reg, offset: Reg) : Reg {
        val ret = newTemp()
        add(InstrLoadArray(size, ret, addr, offset))
        return ret
    }

    fun instrStore(data:Reg, addr:Reg, offset: SymbolField) {
        add(InstrStoreField(offset.type.getSize(), data, addr, offset))
    }

    fun instrLoad(addr:Reg, offset: SymbolField) : Reg {
        val ret = newTemp()
        add(InstrLoadField(offset.type.getSize() , ret, addr, offset))
        return ret
    }

    fun instrStore(data:Reg, offset: SymbolGlobalVar) {
        add(InstrStoreGlobal(offset.type.getSize(), data, offset))
    }

    fun instrLoad(offset: SymbolGlobalVar) : Reg {
        val ret = newTemp()
        add(InstrLoadGlobal(offset.type.getSize() , ret, offset))
        return ret
    }

    fun rebuildIndex() {
        for((index,reg) in regs.withIndex()) {
            reg.index = index
            reg.defs.clear()
            reg.uses.clear()
        }

        for(label in labels)
            label.uses.clear()

        prog.removeIf { it is InstrNop }

        for((index, instr) in prog.withIndex()) {
            instr.index = index
            when (instr) {
                is InstrLabel -> instr.label.index = index
                is InstrBranch -> instr.target.uses += instr
                is InstrJump -> instr.target.uses += instr
                else -> {}
            }

            val def = instr.getDef()
            if (def!=null)
                def.defs.add(instr)

            val uses = instr.getUses()
            for(use in uses)
                use.uses.add(instr)
        }
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


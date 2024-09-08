package backend

import frontend.SymbolField
import frontend.SymbolGlobalVar
import frontend.TcFunction
import frontend.TupleType
import frontend.Type
import frontend.allClassTypes
import frontend.allConstantArrays
import frontend.currentFunction

open class Function(val name:String, val retType:Type, isStdLib:Boolean=false, val isExternal: Boolean=false) {
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
    var makesCalls = false
    var failedNullCheckLabel : Label? = null

    // Values to be filled in by the backend
    var maxRegister = 0    // The highest register number used
    var stackFrameSize = 0


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

    fun instrCall(target: Function, getResult: Boolean=true) : Reg {
        add(InstrCall(target))
        makesCalls = true

        if (getResult) {
            if (target.retType is TupleType) {
                val regs = mutableListOf<Reg>()
                check(target.retType.elementTypes.size <= 4)
                for(index in target.retType.elementTypes.indices) {
                    val reg = newTemp()
                    instrMove(reg, allMachineRegs[8-index])
                    regs += reg
                }
                return TupleReg(regs.joinToString(prefix = "(", postfix = ")"),regs)
            } else {
                val ret = newTemp()
                add(InstrMov(ret, regResult))
                return ret
            }
        } else
            return regZero
    }

    fun instrVirtCall(instance:Reg, target: TcFunction) : Reg {
        add(InstrVirtCall(instance, target))

        val ret : Reg
        if (target.returnType is TupleType) {
            val regs = mutableListOf<Reg>()
            check(target.returnType.elementTypes.size <= 4)
            for(index in target.returnType.elementTypes.indices) {
                val reg = newTemp()
                instrMove(reg, allMachineRegs[8-index])
                regs += reg
            }
            ret = TupleReg(regs.joinToString(prefix = "(", postfix = ")"),regs)
        } else {
            ret = newTemp()
            add(InstrMov(ret, regResult))
        }

        makesCalls = true
        return ret
    }

    fun instrBranch(op: AluOp, lhs: Reg, rhs: Reg, target: Label) {
        add(InstrBranch(op, lhs, rhs, target))
    }

    fun instrLabel(label: Label) {
        add(InstrLabel(label))
    }

    fun instrStore(size:Int, data:Reg, addr:Reg, offset: Int) {
        add(InstrStoreArray(MemSize.toSize(size), data, addr, offset))
    }

    fun instrLoad(size:Int, addr:Reg, offset: Int) : Reg {
        val ret = newTemp()
        add(InstrLoadArray(MemSize.toSize(size), ret, addr, offset))
        return ret
    }

    fun instrStore(data:Reg, addr:Reg, offset: SymbolField) {
        add(InstrStoreField(MemSize.toSize(offset.type.getSize()), data, addr, offset))
    }

    fun instrLoad(addr:Reg, offset: SymbolField) : Reg {
        val ret = newTemp()
        add(InstrLoadField(MemSize.toSize(offset.type.getSize()) , ret, addr, offset))
        return ret
    }

    fun instrStore(data:Reg, offset: SymbolGlobalVar) {
        add(InstrStoreGlobal(MemSize.toSize(offset.type.getSize()), data, offset))
    }

    fun instrLoad(offset: SymbolGlobalVar) : Reg {
        val ret = newTemp()
        add(InstrLoadGlobal(MemSize.toSize(offset.type.getSize()) , ret, offset))
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


    // Allocate space on the stack of size (pre+post)
    // and returns a pointer 'pre' bytes into the allocated region
    fun alloca(pre:Int, post: Int): Reg {
        val offset = stackFrameSize + pre
        stackFrameSize += post + pre
        return instrAlu(AluOp.ADD_I, regSp, offset)
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
    allGlobalVars.clear()
    allClassTypes.clear()
    allConstantArrays.clear()
}


package backend

sealed class Reg(val name:String) {
    var index = 0
    override fun toString() = name
}

class MachineReg(name:String,index:Int) : Reg(name) {
    init {
        this.index = index
    }
}

class UserReg(name:String) : Reg(name)

class TempReg(name:String) : Reg(name)

val allMachineRegs = listOf(
    MachineReg("0", 0),
    MachineReg("%1", 1),
    MachineReg("%2", 2),
    MachineReg("%3", 3),
    MachineReg("%4", 4),
    MachineReg("%5", 5),
    MachineReg("%6", 6),
    MachineReg("%7", 7),
    MachineReg("%8", 8),
    MachineReg("%9", 9),
    MachineReg("%10", 10),
    MachineReg("%11", 11),
    MachineReg("%12", 12),
    MachineReg("%13", 13),
    MachineReg("%14", 14),
    MachineReg("%15", 15),
    MachineReg("%16", 16),
    MachineReg("%17", 17),
    MachineReg("%18", 18),
    MachineReg("%19", 19),
    MachineReg("%20", 20),
    MachineReg("%21", 21),
    MachineReg("%22", 22),
    MachineReg("%23", 23),
    MachineReg("%24", 24),
    MachineReg("%25", 25),
    MachineReg("%26", 26),
    MachineReg("%27", 27),
    MachineReg("%28", 28),
    MachineReg("%29", 29),
    MachineReg("%30", 30),
    MachineReg("%sp", 31),
)

val regZero = allMachineRegs[0]
val regArg1 = allMachineRegs[1]
val regArg2 = allMachineRegs[2]
val regArg3 = allMachineRegs[3]
val regGlobal = allMachineRegs[29]
val regRa = allMachineRegs[30]
val regSp = allMachineRegs[31]
val regResult = allMachineRegs[8]
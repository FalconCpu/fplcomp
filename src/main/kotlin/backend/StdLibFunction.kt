package backend

import frontend.UnitType

sealed class StdLibFunction (
    text : String,
) : Function(text, UnitType,true) {
}

object StdlibPrintInt    : StdLibFunction("print(Int)") {
    fun execute(arg:Value, programOutput: StringBuilder) {
        programOutput.append(arg.getIntValue())
    }
}

object StdlibPrintHex    : StdLibFunction("printHex(Int)") {
    fun execute(arg:Value, programOutput: StringBuilder) {
        programOutput.append(arg.getIntValue().toString(16))
    }
}

object StdlibPrintChar   : StdLibFunction("print(Char)") {
    fun execute(arg:Value, programOutput: StringBuilder) {
        programOutput.append(arg.getIntValue().toChar())
    }
}

object StdlibPrintString : StdLibFunction("print(String)") {
    fun execute(arg: Value, programOutput: StringBuilder) {
        programOutput.append(arg.getStringValue())
    }
}

object StdlibPrintBool   : StdLibFunction("print(Bool)") {
    fun execute(arg: Value, programOutput: StringBuilder) {
        programOutput.append(if (arg.getIntValue() == 0) "false" else "true")
    }
}

object StdlibNewline     : StdLibFunction("printNewline()") {
    fun execute(programOutput: StringBuilder) {
        programOutput.append("\n")
    }
}

object StdlibMalloc : StdLibFunction("malloc(Int,Pointer)")

object StdlibFatal       : StdLibFunction("fatal(Int,Int)")
object StdlibStrcat      : StdLibFunction("strcat(String,String)")
object StdlibStrcmp      : StdLibFunction("strcmp(String,String)")
object StdlibStrequals      : StdLibFunction("strequals(String,String)")

object StdlibPremain      : StdLibFunction("premain()") {
    fun execute(arg1: Value, arg2: Value): Value {
        return StringValue(arg1.getStringValue() + arg2.getStringValue())
    }
}
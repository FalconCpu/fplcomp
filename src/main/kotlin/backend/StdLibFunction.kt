package backend

sealed class StdLibFunction (
    text : String,
) : Function(text, true) {
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

object StdlibMallocArray : StdLibFunction("mallocArray(Int,Int)") {
    fun execute(arg: Value) : Value {
        return ArrayValue(Array(arg.getIntValue()) { UndefinedValue })
    }
}

object StdlibMallocObject: StdLibFunction("mallocObject(ClassDescriptor)") {
    fun execute(arg: Value) : Value {
        val type = (arg as ClassRefValue).classRef
        return ClassValue(arg.classRef, Array(type.numFields){ UndefinedValue})
    }
}

object StdlibStrcat      : StdLibFunction("strcat") {
    fun execute(arg1: Value, arg2: Value): Value {
        return StringValue(arg1.getStringValue() + arg2.getStringValue())
    }
}

object StdlibPremain      : StdLibFunction("premain()") {
    fun execute(arg1: Value, arg2: Value): Value {
        return StringValue(arg1.getStringValue() + arg2.getStringValue())
    }
}
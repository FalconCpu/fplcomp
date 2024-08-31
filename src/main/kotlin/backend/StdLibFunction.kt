package backend

sealed class StdLibFunction (
    name : String,
) : Function(name, true)

data object StdlibPrintInt    : StdLibFunction("printInt") {
    fun execute(arg:Value, programOutput: StringBuilder) {
        programOutput.append(arg.getIntValue())
    }
}

data object StdlibPrintChar   : StdLibFunction("printChar") {
    fun execute(arg:Value, programOutput: StringBuilder) {
        programOutput.append(arg.getIntValue().toChar())
    }
}

data object StdlibPrintString : StdLibFunction("printString") {
    fun execute(arg: Value, programOutput: StringBuilder) {
        programOutput.append(arg.getStringValue())
    }
}

data object StdlibPrintBool   : StdLibFunction("printBool") {
    fun execute(arg: Value, programOutput: StringBuilder) {
        programOutput.append(if (arg.getIntValue() == 0) "false" else "true")
    }
}

data object StdlibNewline     : StdLibFunction("printNewline") {
    fun execute(programOutput: StringBuilder) {
        programOutput.append("\n")
    }
}

data object StdlibMallocArray : StdLibFunction("mallocArray") {
    fun execute(arg: Value) : Value {
        return ArrayValue(Array(arg.getIntValue()) { UndefinedValue })
    }
}

data object StdlibMallocObject: StdLibFunction("mallocObject") {
    fun execute(arg: Value) : Value {
        val type = (arg as ClassRefValue).classRef
        return ClassValue(arg.classRef, Array(type.numFields){ UndefinedValue})
    }
}

data object StdlibStrcat      : StdLibFunction("strcat") {
    fun execute(arg1: Value, arg2: Value): Value {
        return StringValue(arg1.getStringValue() + arg2.getStringValue())
    }
}
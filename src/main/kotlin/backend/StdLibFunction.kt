package backend

sealed class StdLibFunction (
    name : String,
) : Function(name, true)

data object StdlibPrintInt    : StdLibFunction("printInt")
data object StdlibPrintChar   : StdLibFunction("printChar")
data object StdlibPrintString : StdLibFunction("printString")
data object StdlibPrintBool   : StdLibFunction("printBool")
data object StdlibNewline     : StdLibFunction("printNewline")
data object StdlibMallocArray : StdLibFunction("mallocArray")
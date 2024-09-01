package backend

import frontend.SymbolGlobalVar

val allFunctions = mutableListOf<Function>()
val allGlobalVars = mutableListOf<SymbolGlobalVar>()

fun Function.runBackend(currentFunction: Function) {

    Peephole(currentFunction).run()

    val livemap = Livemap(currentFunction)
    RegisterAllocator(currentFunction, livemap).run()

    Peephole(currentFunction).run()

}


fun runBackend() {
    for (function in allFunctions)
        function.runBackend(function)
}
package backend

val allFunctions = mutableListOf<Function>()

fun Function.runBackend(currentFunction: Function) {

    Peephole(currentFunction).run()

    val livemap = Livemap(currentFunction)
    livemap.dump()
    RegisterAllocator(currentFunction, livemap).run()

    Peephole(currentFunction).run()

}


fun runBackend() {
    for (function in allFunctions)
        function.runBackend(function)
}
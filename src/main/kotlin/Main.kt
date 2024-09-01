import frontend.AstTop
import frontend.Lexer
import frontend.Parser
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

fun main() {
    println("Hello World!")
}

enum class StopAt {
    AST,
    TYPECHECK,
    IRGEN,
    REGALLOC,
    ASMGEN
}

fun compile(files:List<Lexer>, stopAt: StopAt) : String {
    Log.initialize()
    backend.initialize()

    // Parse
    val top = AstTop()
    for(file in files)
        Parser(file).parseTop(top)
    if (Log.anyError())
        return Log.dump()
    if (stopAt == StopAt.AST)
        return top.dump()

    // Typecheck
    top.identifyFunctions(top)
    val tcTop = top.typeCheck()
    if (Log.anyError())
        return Log.dump()
    if (stopAt == StopAt.TYPECHECK)
        return tcTop.dump()

    // IRGen
    tcTop.codeGen()
    if (Log.anyError())
        return Log.dump()
    if (stopAt == StopAt.IRGEN)
        return backend.dumpAllFunctions()

    // RegAlloc
    backend.runBackend()
    if (Log.anyError())
        return Log.dump()
    if (stopAt == StopAt.REGALLOC)
        return backend.dumpAllFunctions()

    // AsmGen
    val asm = backend.asmGen()
    if (Log.anyError())
        return Log.dump()
    return asm
}

fun String.runCommand(): String? {
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
        return null
    }
}
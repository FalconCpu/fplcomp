import frontend.AstTop
import frontend.Lexer
import frontend.Parser

fun main() {
    println("Hello World!")
}

enum class StopAt {
    AST,
    TYPECHECK,
    IRGEN,
    ASMGEN,
    ALL
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

    TODO()
}
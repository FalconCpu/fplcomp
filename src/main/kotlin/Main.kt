package falcon

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

    // Parse
    val top = AstBlockTop()
    for(file in files)
        Parser(file).parseTop(top)
    if (Log.anyError())
        return Log.dump()
    if (stopAt == StopAt.AST)
        return top.dump()

    // Typecheck
    val tcTop = top.typeCheck()
    if (Log.anyError())
        return Log.dump()
    if (stopAt == StopAt.TYPECHECK)
        return tcTop.dump()

    TODO()
}
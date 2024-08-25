package falcon

// The PathContext class is used to track the current state of variables along different control paths.

// Note: PathContexts are managed using global variables for convenience.
// While using global variables can be less ideal, it simplifies the management of path contexts
// compared to passing them around through numerous function calls. PathContexts themselves are immutable,
// (copies are made when changes are made),
// but we use these global variables to keep track of the current state across different parts of the code.

// The currentPathContext variable holds the PathContext for the currently executing path.
// It is updated by genCodeStatement() and reflects the state of variable initialization and smartcasts
var currentPathContext = PathContext()

// Where control flow branches, the trueBranchContext and falseBranchContext variables are used to track
// the state of variables along each branch of the control flow. These are updated by genCodeBool()
// trueBranchContext is updated to reflect the state of the path if the condition evaluates to true.
// falseBranchContext is updated to reflect the state if the condition evaluates to false.
var trueBranchContext = currentPathContext
var falseBranchContext = currentPathContext

// Usage in code:
// - In AstIf, AstWhile, are responsible for copying trueBranchContext/falseBranchContext into currentPathContext
// as appropriate before generating the code for the body of the conditional.
// And then calling merge on the currentPathContext at the end of each body before continuing.

// This approach ensures that variable initialization and smartcast states are accurately tracked
// across different paths of execution without the need to pass the PathContext explicitly through every function call.


data class PathContext (
    val uninitializedVariables: Set<Symbol> = emptySet(),
    val maybeUninitializedVariables: Set<Symbol> = emptySet(),
    val smartCasts: Map<Symbol,Type> = emptyMap(),
    val isReachable : Boolean = true
) {

    fun initializeVariable(symbol: Symbol) =
        if (symbol in uninitializedVariables || symbol in maybeUninitializedVariables)
            PathContext(
                uninitializedVariables = uninitializedVariables - symbol,
                maybeUninitializedVariables = maybeUninitializedVariables - symbol,
                smartCasts = smartCasts,
                isReachable = isReachable
            )
        else
            this


    fun addUninitializedVariable(symbol: Symbol) =
        PathContext(
            uninitializedVariables = uninitializedVariables + symbol,
            maybeUninitializedVariables = maybeUninitializedVariables + symbol,
            smartCasts = smartCasts,
            isReachable = isReachable
        )

    fun addSmartCast(symbol: Symbol?, type: Type) =
        if (symbol!=null && smartCasts[symbol]!=type)
            PathContext(
                uninitializedVariables = uninitializedVariables,
                maybeUninitializedVariables = maybeUninitializedVariables,
                smartCasts = smartCasts + (symbol to type),
                isReachable = isReachable
            )
        else
            this

    fun setUnreachable() =
        if (isReachable)
            PathContext(
                uninitializedVariables = uninitializedVariables,
                maybeUninitializedVariables = maybeUninitializedVariables,
                smartCasts = smartCasts,
                isReachable = false
            )
        else
            this

    fun merge(other: PathContext) =
        if (this!=other)
            PathContext(
                uninitializedVariables = uninitializedVariables intersect other.uninitializedVariables,
                maybeUninitializedVariables = maybeUninitializedVariables + other.maybeUninitializedVariables,
                smartCasts = smartCasts.filter {
                        (symbol, type) -> other.smartCasts[symbol] == type
                },
                isReachable = isReachable && other.isReachable

            )
        else
            this

    fun removeSmartcast(symbol: Symbol) =
        if (symbol in smartCasts.keys)
            PathContext(
                uninitializedVariables = uninitializedVariables,
                maybeUninitializedVariables = maybeUninitializedVariables,
                smartCasts = smartCasts - symbol,
                isReachable = isReachable
            )
        else
            this
}

fun mergePathContext(paths: List<PathContext>): PathContext {
    val reachablePaths = paths.filter { it.isReachable }
    return if (reachablePaths.isEmpty())
        paths[0]  // None of the paths are reachable, so we can return any of them
    else if (reachablePaths.size == 1)
        reachablePaths[0] // Only one path is reachable, so we can return that one
    else
        reachablePaths.reduce { acc, path -> acc.merge(path) }
}



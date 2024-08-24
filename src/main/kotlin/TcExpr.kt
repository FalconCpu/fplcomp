package falcon

sealed class TcExpr(
    val location: Location,
    val type: Type
) {
    abstract fun dump(sb: StringBuilder, indent: Int)
}
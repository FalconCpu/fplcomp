package falcon

sealed class TcStmt(
    location: Location
) {
    abstract fun dump(sb: StringBuilder, indent: Int)
}
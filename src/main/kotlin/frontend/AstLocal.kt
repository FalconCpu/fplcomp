package frontend

class AstLocal (
    location: Location,
    val value: AstExpr
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("LOCAL\n")
        value.dump(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock): TcExpr {
        val value = value.typeCheck(context)
        return when(value) {
            is TcArrayConstructor -> value . also {it.localAlloc = true }
            is TcArrayOf -> value . also {it.localAlloc = true }
            is TcConstructor -> value . also {it.localAlloc = true }
            else -> TcError(location, "'local' can only be used on constructors")
        }
    }
}
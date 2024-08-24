package falcon

class AstParameter(
    location: Location,
    private val kind: TokenKind,
    private val name: String,
    private val type: AstType
) : Ast(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        val kindTxt = if (kind==TokenKind.EOF) "" else " $kind"
        sb.append(". ".repeat(indent))
        sb.append("PARAMETER $name$kindTxt\n")
        type.dump(sb, indent + 1)
    }

}
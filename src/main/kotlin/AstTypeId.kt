package falcon

class AstTypeId(
    location: Location,
    private val name: String
) : AstType(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("TYPEID $name\n")
    }
}
package falcon

class AstEnum (
    location: Location,
    parent: AstBlock,
    private val name: String,
    private val astValues: List<AstIdentifier>
) : AstBlock(location, parent) {

    val type = makeEnumType(name, this)
    private val symbol = SymbolTypeName(location, name, type)

    init {
        parent.add(symbol)
    }

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ENUM $name\n")
        astValues.forEach { it.dump(sb, indent + 1) }
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ENUM $name\n")
    }

    override fun typeCheck(context: AstBlock) {

    }

    override fun identifyFunctions(context: AstBlock) {
        for((index,value) in astValues.withIndex()) {
            val sym = SymbolLiteral(value.location, value.name, type, index)
            add(sym)
        }
    }
}
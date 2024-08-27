package frontend

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

    private lateinit var tcEnum: TcEnum    // filled in at identifyFunctions()

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ENUM $name\n")
        astValues.forEach { it.dump(sb, indent + 1) }
    }

    override fun identifyFunctions(context: AstBlock) {
        tcEnum = TcEnum(location, symbolTable, name)
        for((index,value) in astValues.withIndex()) {
            val sym = SymbolLiteral(value.location, value.name, type, index)
            symbolTable.add(sym)
        }
    }

    override fun typeCheck(context: AstBlock) : TcBlock {
        return tcEnum
    }


}

class TcEnum (
    location: Location,
    symbolTable: SymbolTable,
    private val name: String
) : TcBlock(location, symbolTable) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("ENUM $name\n")
    }

    override fun codeGen() {
        // Nothing needed
    }
}

package falcon

sealed class Symbol(
    val location: Location,
    val name: String,
    val type: Type,
) {
    override fun toString() = name

    fun description() = when(this) {
        is SymbolFunctionName -> "FUNC $name $type"
        is SymbolGlobalVar -> "GLOBALVAR $name $type"
        is SymbolLocalVar -> "LOCALVAR $name $type"
        is SymbolTypeName -> "TYPENAME $name $type"
        is SymbolLiteral -> "LITERAL $name $type"
        is SymbolField -> "FIELD $name $type"
    }
}

class SymbolLocalVar(
    location: Location,
    name: String,
    type: Type,
    val isMutable: Boolean
) : Symbol(location, name, type)

class SymbolGlobalVar(
    location: Location,
    name: String,
    type: Type,
    val isMutable: Boolean
) : Symbol(location, name, type)

class SymbolFunctionName(
    location: Location,
    name: String,
    type: Type,
) : Symbol(location, name, type)

class SymbolTypeName(
    location: Location,
    name: String,
    type: Type,
) : Symbol(location, name, type)

class SymbolLiteral(
    location: Location,
    name: String,
    type: Type,
    val value: Int
) : Symbol(location, name, type)

class SymbolField(
    location: Location,
    name: String,
    type: Type,
    val mutable : Boolean
) : Symbol(location, name, type)
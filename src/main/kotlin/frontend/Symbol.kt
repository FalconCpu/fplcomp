package frontend

import backend.Value

sealed class Symbol(
    val location: Location,
    val name: String,
    val type: Type,
) {
    override fun toString() = name

    fun description() = when(this) {
        is SymbolFunctionName -> "FUNC $name"
        is SymbolGlobalVar -> "GLOBALVAR $name"
        is SymbolLocalVar -> "LOCALVAR $name"
        is SymbolTypeName -> "TYPENAME $name"
        is SymbolLiteral -> "LITERAL $name"
        is SymbolField -> "FIELD $name"
        is SymbolMemberAccess -> "MEMBERACCESS $name"
    }

    fun isMutable() : Boolean = when (this) {
        is SymbolLocalVar -> mutable
        is SymbolGlobalVar -> mutable
        is SymbolMemberAccess -> rhs.isMutable()
        is SymbolField -> mutable
        else -> false
    }

    companion object {
        const val UNDEFINED_OFFSET = -2
    }
}

class SymbolLocalVar(
    location: Location,
    name: String,
    type: Type,
    val mutable: Boolean
) : Symbol(location, name, type)

class SymbolGlobalVar(
    location: Location,
    name: String,
    type: Type,
    val mutable: Boolean
) : Symbol(location, name, type) {
    var offset = UNDEFINED_OFFSET
}

class SymbolFunctionName(
    location: Location,
    name: String,
) : Symbol(location, name, UnitType) {
    val overloads = mutableListOf<TcFunction>()

    fun clone() : SymbolFunctionName {
        val clone = SymbolFunctionName(location, name)
        clone.overloads.addAll(overloads)
        return clone
    }
}

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
) : Symbol(location, name, type) {
    var offset = UNDEFINED_OFFSET
}


// Symbol used to track smartcasts through member accesses
class SymbolMemberAccess(
    location: Location,
    val lhs : Symbol,
    val rhs: Symbol
) : Symbol(location, "$lhs.$rhs", rhs.type) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolMemberAccess) return false

        if (lhs != other.lhs) return false
        if (rhs != other.rhs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lhs.hashCode()
        result = 31 * result + rhs.hashCode()
        return result
    }
}


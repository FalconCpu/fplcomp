package falcon

sealed class Type (val name:String) {
    override fun toString() = name

    fun isAssignCompatible(type: Type) : Boolean {

        if (this == type) return true

        if (this is ErrorType || type is ErrorType) return true // Allow errors to propagate silently

        if (this is NullableType && type is NullType) return true // Allow null to be assigned to T?
        if (this is NullableType) return base.isAssignCompatible(type) // Allow T to be assigned to T?

        // TODO - sub types of structs

        return false
    }

    fun checkAssignCompatible(location: Location, type: Type) {
        if (!isAssignCompatible(type))
            Log.error(location, "Got type $type when expecting $this")
    }

    fun getSize(): Int = when (this) {
        CharType -> 1
        UnitType -> 0
        BoolType -> 4
        is ArrayType -> 4
        is ClassType -> 4
        ErrorType -> 4
        is FunctionType -> 4
        IntType -> 4
        NullType -> 4
        is NullableType -> 4
        RealType -> 4
        StringType -> 4
        PointerType -> 4
    }
}

// ---------------------------------------------------------------------
//                           Primitives
// ---------------------------------------------------------------------

object NullType : Type("Null")
object UnitType : Type("Unit")
object BoolType : Type("Bool")
object CharType : Type("Char")
object IntType : Type("Int")
object RealType : Type("Real")
object StringType : Type("String")
object ErrorType : Type("Error")
object PointerType : Type("Pointer")  // Only used in the backend - user code never sees this



// ---------------------------------------------------------------------
//                           Arrays
// ---------------------------------------------------------------------

class ArrayType(val elementType: Type) :
    Type("Array<$elementType>")

val allArrayTypes = mutableListOf<ArrayType>()

fun makeArrayType(elementType: Type): ArrayType {
    return allArrayTypes.find { it.elementType == elementType} ?: run {
        val new = ArrayType(elementType)
        allArrayTypes.add(new)
        new
    }
}

// ---------------------------------------------------------------------
//                           Function Types
// ---------------------------------------------------------------------

class FunctionType(val paramTypes: List<Type>, val retType: Type) :
    Type("(${paramTypes.joinToString(",")})->$retType")

val allFunctionTypes = mutableListOf<FunctionType>()

fun makeFunctionType(paramTypes: List<Type>, retType: Type): FunctionType {
    return allFunctionTypes.find { it.paramTypes == paramTypes && it.retType == retType } ?: run {
        val new = FunctionType(paramTypes, retType)
        allFunctionTypes.add(new)
        new
    }
}

// ---------------------------------------------------------------------
//                           Class Types
// ---------------------------------------------------------------------

class ClassType(name: String) : Type(name) {
    //lateinit var function: Function
    var structSize = 0
}

val allClassTypes = mutableListOf<ClassType>()

fun makeClassType(name: String): ClassType {
    val new = ClassType(name)
    allClassTypes.add(new)
    return new
}

// ---------------------------------------------------------------------
//                           Nullable Types
// ---------------------------------------------------------------------

class NullableType(val base: Type) : Type("$base?")

val allNullableTypes = mutableListOf<NullableType>()

fun makeNullableType(location: Location, base: Type): Type {
    if (base is NullableType || base is ErrorType) return base
    if (base is IntType || base is RealType || base is BoolType || base is CharType)
        return makeTypeError(location, "Cannot make nullable type of primitive type $base")

    return allNullableTypes.find { it.base == base } ?: run {
        val new = NullableType(base)
        allNullableTypes.add(new)
        new
    }
}

// ---------------------------------------------------------------------
//                           Error
// ---------------------------------------------------------------------

fun makeTypeError(location: Location, message:String): Type {
    Log.error(location, message)
    return ErrorType
}


// ---------------------------------------------------------------------
//                           Predefined Symbols
// ---------------------------------------------------------------------

val predefinedSymbols = createPredefinedSymbols()

private fun createPredefinedSymbols(): SymbolTable {
    val symbolTable = SymbolTable(null)
    for (type in listOf(NullType, UnitType, BoolType, CharType, IntType, RealType, StringType)) {
        val sym = SymbolTypeName(nullLocation, type.name, type)
        symbolTable.add(sym)
    }
    symbolTable.add(SymbolLiteral(nullLocation, "null", NullType, 0))
    symbolTable.add(SymbolLiteral(nullLocation, "true", BoolType, 1))
    symbolTable.add(SymbolLiteral(nullLocation, "false", BoolType, 0))
    return symbolTable
}

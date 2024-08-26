package frontend

sealed class Type (val name:String) {
    override fun toString() = name

    /**
     * Test to see if a value of type rhsType can be assigned to a variable of this type
     */
    fun isAssignCompatible(rhsType: Type) : Boolean {

        if (this == rhsType) return true

        if (this is ErrorType || rhsType is ErrorType) return true // Allow errors to propagate silently

        if (this is NullableType && rhsType is NullType) return true // Allow null to be assigned to T?

        if (this is NullableType) return base.isAssignCompatible(rhsType) // Allow T to be assigned to T?

        if (this is ClassType && rhsType is ClassType) return rhsType.isSubTypeOf(this)

        return false
    }

    fun checkAssignCompatible(location: Location, type: Type) {
        if (!isAssignCompatible(type))
            Log.error(location, "Got type $type when expecting $this")
    }

    fun makeNonNull(): Type = when (this) {
        is NullableType -> base
        else -> this
    }

//    fun getSize(): Int = when (this) {
//        CharType -> 1
//        UnitType -> 0
//        BoolType -> 4
//        is ArrayType -> 4
//        is ClassType -> 4
//        ErrorType -> 4
//        is FunctionType -> 4
//        IntType -> 4
//        NullType -> 4
//        is NullableType -> 4
//        RealType -> 4
//        StringType -> 4
//    }
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
//                           backend.Function Types
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

class ClassType(name: String, val definition:AstClass, val superClass: ClassType?) : Type(name) {

    fun isSubTypeOf(superclass: ClassType): Boolean {
        if (this == superclass) return true
        if (superClass == null) return false
        return superClass.isSubTypeOf(superclass)
    }
}

val allClassTypes = mutableListOf<ClassType>()

fun makeClassType(name: String, definition: AstClass, superClass: ClassType?): ClassType {
    val new = ClassType(name, definition, superClass)
    allClassTypes.add(new)
    return new
}

// ---------------------------------------------------------------------
//                           Enum Types
// ---------------------------------------------------------------------

class EnumType(name: String, val definition: AstEnum) : Type(name)

fun makeEnumType(name: String, definition: AstEnum): EnumType {
    return EnumType(name, definition)
}


// ---------------------------------------------------------------------
//                           Nullable Types
// ---------------------------------------------------------------------

class NullableType(val base: Type) : Type("$base?")

val allNullableTypes = mutableListOf<NullableType>()

fun makeNullableType(base: Type): Type {
    if (base is NullableType || base is ErrorType) return base

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

private fun createPredefinedSymbols(): AstTop {
    val symbolTable = AstTop()
    for (type in listOf(NullType, UnitType, BoolType, CharType, IntType, RealType, StringType)) {
        val sym = SymbolTypeName(nullLocation, type.name, type)
        symbolTable.add(sym)
    }
    symbolTable.add(SymbolLiteral(nullLocation, "null", NullType, 0))
    symbolTable.add(SymbolLiteral(nullLocation, "true", BoolType, 1))
    symbolTable.add(SymbolLiteral(nullLocation, "false", BoolType, 0))
    return symbolTable
}

package frontend

import backend.Reg

class AstIdentifier (
    location: Location,
    val name: String
) : AstExpr(location){

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IDENTIFIER $name\n")
    }

    private fun makeNewSymbol(symbolTable: AstBlock): Symbol {
        Log.error(location,"Undefined identifier: $name")
        val symbol = SymbolLocalVar(location, name, ErrorType, true)
        symbolTable.add(symbol)
        return symbol
    }

    override fun typeCheckAllowTypeName(context: AstBlock) : TcIdentifier {
        val symbol = predefinedSymbols.lookup(name) ?: context.lookup(name) ?: makeNewSymbol(context)
        val type = currentPathContext.smartCasts[symbol] ?: symbol.type

        if (symbol in currentPathContext.uninitializedVariables)
            Log.error(location, "Variable '$name' has not been initialized")
        else if (symbol in currentPathContext.maybeUninitializedVariables)
            Log.error(location, "Variable '$name' might not be initialized")

        return TcIdentifier(location, type, symbol)
    }

    override fun typeCheck(context:AstBlock) : TcExpr {
        // Symbol has been used as an rvalue
        val ret = typeCheckAllowTypeName(context)
        if (ret.symbol is SymbolTypeName)
            Log.error(location, "Got type name '$ret' when expecting a value")
        return ret
    }


    override fun typeCheckLvalue(context: AstBlock) : TcExpr {
        // Symbol has been used as an lvalue
        val symbol = predefinedSymbols.lookup(name) ?: context.lookup(name) ?: makeNewSymbol(context)
        val type = symbol.type

        when(symbol) {
            is SymbolField ->
                if (!symbol.mutable)
                    Log.error(location, "Global variable $name is not mutable")

            is SymbolGlobalVar ->
                if (!symbol.mutable)
                    Log.error(location, "Global variable $name is not mutable")

            is SymbolLocalVar ->
                if (!symbol.mutable && symbol !in currentPathContext.uninitializedVariables)
                    Log.error(location, "Local variable $name is not mutable")

            is SymbolMemberAccess,
            is SymbolFunctionName,
            is SymbolLiteral,
            is SymbolTypeName -> Log.error(location, "Not an lvalue: $name")
        }

        currentPathContext = currentPathContext.initializeVariable(symbol)
        return TcIdentifier(location, type, symbol)
    }

}

class TcIdentifier(
    location: Location,
    type : Type,
    val symbol : Symbol
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("IDENTIFIER ${symbol.description()} $type\n")
    }

    override fun toString() = symbol.toString()

    override fun isMutable() = symbol.isMutable()


    override fun codeGenRvalue(): Reg {
        return when(symbol) {
            is SymbolLocalVar -> currentFunction.getReg(symbol)
            is SymbolField -> currentFunction.instrLoad(currentFunction.getThis(), symbol)
            is SymbolFunctionName -> TODO("Function names as rvalues")
            is SymbolGlobalVar -> currentFunction.instrLoad(symbol)
            is SymbolLiteral -> currentFunction.instrInt(symbol.value)
            is SymbolMemberAccess,
            is SymbolTypeName -> error("Got kind ${symbol.javaClass} in codeGenRvalue")
        }
    }

    override fun codeGenLvalue(value: Reg) {
        when (symbol) {
            is SymbolLocalVar -> currentFunction.instrMove( currentFunction.getReg(symbol), value)
            is SymbolField -> currentFunction.instrStore(value, currentFunction.thisReg!!, symbol)
            is SymbolGlobalVar -> currentFunction.instrStore(value, symbol)
            is SymbolFunctionName,
            is SymbolLiteral,
            is SymbolMemberAccess,
            is SymbolTypeName -> error("Got kind ${symbol.javaClass} in codeGenLvalue")
        }
    }

}

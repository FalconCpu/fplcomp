package frontend

import backend.TupleReg
import backend.allGlobalVars

class AstDeclaration (
    location: Location,
    private val op : TokenKind,
    private val names: List<AstIdentifier>,
    private val type: AstType?,
    private val value: AstExpr?
) : AstStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        if (names.size==1)
            sb.append("$op ${names[0]}\n")
        else
            sb.append("$op $names\n")
        type?.dump(sb, indent + 1)
        value?.dump(sb, indent + 1)
    }

    private fun errorDeclaration(context: AstBlock, message:String) : TcStmt {
        Log.error(location, message)
        val symbols = names.map { SymbolLocalVar(it.location, it.name, ErrorType, true) }
        symbols.forEach{ context.add(it)}
        return TcDeclaration(location, symbols, null)
    }

    private fun genSymbol(location: Location, name:String, type: Type, mutable: Boolean, context: AstBlock) =
        when (context) {
            is AstTop   -> SymbolGlobalVar(location, name, type, mutable)
            is AstClass -> SymbolField(location, name, type, mutable)
            else             -> SymbolLocalVar(location, name, type, mutable)
        }

    private fun typeCheckDestructuringDecl(context: AstBlock) : TcStmt {
        if (type!=null)
            Log.error(location, "Explicit types not supported for destructuring declarations")
        if (value==null)
            return errorDeclaration(context, "Destructuring declarations must have an initializer")

        val value = value.typeCheck(context)
        if (value.type !is TupleType)
            return errorDeclaration(context, "Destructuring declarations must have a tuple initializer")
        if (names.size != value.type.elementTypes.size)
            return errorDeclaration(context,
                "Destructuring declarations must have the same number of elements as the tuple initializer")

        val mutable = (op == TokenKind.VAR)
        val symbols = names.mapIndexed { index, name ->
            genSymbol(name.location, name.name, value.type.elementTypes[index], mutable, context)
        }

        symbols.forEach{ context.add(it) }
        return TcDeclaration(location, symbols, value)
    }

    override fun typeCheck(context:AstBlock) : TcStmt {
        if (names.size!=1)
            return typeCheckDestructuringDecl(context)

        // Handle the case of a single declaration

        val name = names[0].name
        val value = value?.typeCheck(context)
        val type =  type?.resolveType(context) ?:
                    value?.type ?:
                    makeTypeError(location, "Unknown type for $name")
        val mutable = (op == TokenKind.VAR)

        if (type is TupleType)
            Log.error(location, "Variables are not allowed to have tuple types")

        val symbol = genSymbol(location, name, type, mutable, context)
        context.add(symbol)

        if (symbol is SymbolGlobalVar) {
            symbol.offset = allGlobalVars.size
            allGlobalVars.add(symbol)
        }

        if (value != null)
            type.checkAssignCompatible(value.location, value)
        else
            currentPathContext = currentPathContext.addUninitializedVariable(symbol)
        return TcDeclaration(location, listOf(symbol), value)
    }

}

class TcDeclaration (
    location: Location,
    private val symbol: List<Symbol>,
    private val value: TcExpr?
) : TcStmt(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        if (symbol.size == 1)
            sb.append("DECL ${symbol[0].description()} ${symbol[0].type}\n")
        else
            sb.append("DECL $symbol\n")
        value?.dump(sb, indent + 1)
    }

    override fun codeGen() {
        if (value==null)
            return

        val value = value.codeGenRvalue()

        for(index in symbol.indices) {
            val sym = symbol[index]
            val rhs = if (value is TupleReg) value.regs[index] else value

            when (sym) {
                is SymbolLocalVar -> currentFunction.instrMove(currentFunction.getReg(sym), rhs)
                is SymbolField -> currentFunction.instrStore(rhs, currentFunction.thisReg!!, sym)
                is SymbolGlobalVar -> currentFunction.instrStore(rhs, sym)
                is SymbolFunctionName,
                is SymbolLiteral,
                is SymbolMemberAccess,
                is SymbolTypeName -> error("Got ${this.javaClass} in AstDeclaration")
            }
        }
    }
}

package frontend

import backend.*

class AstFuncCall (
    location: Location,
    private val lhs: AstExpr,
    private val args: List<AstExpr>
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("FUNCCALL\n")
        lhs.dump(sb, indent + 1)
        for (arg in args) {
            arg.dump(sb, indent + 1)
        }
    }

    private fun typeCheckFunctionCall(funcSym: SymbolFunctionName, thisExpr:TcExpr?, args:List<TcExpr>) : TcExpr {
        val argTypes = args.map { it.type }
        val match = funcSym.overloads.filter { it.matchParams(argTypes)}
        if (match.isEmpty())
            return TcError(location, "No functions match $funcSym(${argTypes.joinToString()}). Possibilities are:-\n" +
                    funcSym.overloads.joinToString(prefix="                  ", separator = "\n"){ it.toString() })

        if (match.size > 1)
            return TcError(location, "Ambiguous overloads for $funcSym(${argTypes.joinToString()})\nPossibilities are:\n" +
                        match.joinToString(prefix = "                  ", separator = "\n") { it.toString() })

        val func = match.single()

        return TcFuncCall(location, func.returnType, func , args, thisExpr)
    }

    private fun typeCheckConstructor(type:Type, args:List<TcExpr>) : TcExpr {
        if (type !is ClassType)
            return TcError(location, "Cannot call constructor for type '$type'")
        if (type.isAbstract)
            Log.error(location, "Cannot call constructor for abstract class '$type'")

        val params = type.tcClass.constructorParameters
        checkArgListSymbol(location, params, args)
        return TcConstructor(location, type, args)
    }


    override fun typeCheck(context: AstBlock) : TcExpr {
        val lhs = lhs.typeCheckAllowType(context)
        val args = args.map { it.typeCheck(context) }

        if (lhs.type == ErrorType)
            return lhs
        if (lhs is TcIdentifier && lhs.symbol is SymbolFunctionName)
            return typeCheckFunctionCall(lhs.symbol, null, args)
        if (lhs is TcIdentifier && lhs.symbol is SymbolTypeName)
            return typeCheckConstructor(lhs.symbol.type, args)
        if (lhs is TcMemberAccess && lhs.symbol is SymbolFunctionName)
            return typeCheckFunctionCall(lhs.symbol, lhs.lhs, args)
        return TcError(lhs.location, "Not a function to call")
    }

}

class TcFuncCall (
    location: Location,
    type: Type,
    private val func: TcFunction,
    private val args: List<TcExpr>,
    private val thisArg: TcExpr?
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("FUNCCALL $func $type\n")
        thisArg?.dump(sb, indent + 1)
        for (arg in args) {
            arg.dump(sb, indent + 1)
        }
    }

    override fun codeGenRvalue(): Reg {
        val args = args.map { it.codeGenRvalue() }


        val thisReg = if (thisArg!=null)
            thisArg.codeGenRvalue() else currentFunction.thisReg

        var argIndex = 1
        if (func.thisSymbol!=null)
            currentFunction.instrMove(allMachineRegs[argIndex++], thisReg!!)

        for (arg in args)
            currentFunction.instrMove(allMachineRegs[argIndex++], arg)

        return if (func.methodKind == MethodKind.NONE)
            currentFunction.instrCall(func.backendFunction)
        else
            currentFunction.instrVirtCall(thisReg!!, func)
    }
}

class TcConstructor(
    location: Location,
    type: Type,
    private val args: List<TcExpr>
) : TcExpr(location, type) {

    var localAlloc = false      // true value gets poked in here if we are instancing a local object

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CONSTRUCTOR $type\n")
        for (arg in args) {
            arg.dump(sb, indent + 1)
        }
    }

    override fun codeGenRvalue(): Reg {
        require(type is ClassType)

        val ret : Reg
        if (localAlloc) {
            // Allocate memory for the object on the stack
            ret = currentFunction.alloca(4, type.getStructSize())
            currentFunction.instrLea(regArg1,type)
            currentFunction.instrStore(4, regArg1, ret, -4)
        } else {
            // Malloc the memory
            currentFunction.instrMove(regArg1, type.getStructSize())
            currentFunction.instrLea(regArg2, type)
            ret = currentFunction.instrCall(StdlibMalloc)
        }

        // Call the constructor
        val args = args.map { it.codeGenRvalue() }
        var argIndex = 1
        currentFunction.instrMove( allMachineRegs[argIndex++], ret)   // Setup 'this'
        for (arg in args)
            currentFunction.instrMove(allMachineRegs[argIndex++], arg)
        currentFunction.instrCall(type.constructor)

        return ret
    }
}

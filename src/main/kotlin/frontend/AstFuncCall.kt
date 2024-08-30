package frontend

import backend.*

class AstFuncCall (
    location: Location,
    private val func: AstExpr,
    private val args: List<AstExpr>
) : AstExpr(location) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("FUNCCALL\n")
        func.dump(sb, indent + 1)
        for (arg in args) {
            arg.dump(sb, indent + 1)
        }
    }

    private fun typeCheckFunctionCall(func:TcExpr, args:List<TcExpr>) : TcExpr {
        require(func.type is FunctionType)
        val paramTypes = func.type.paramTypes
        checkArgList(location, paramTypes, args)

        return if (func is TcMemberAccess && func.symbol is SymbolFunctionName)
            // got a method call of the form expr.funcname
            TcFuncCall(location, func.type.retType, func.symbol , args, func.lhs)
        else if (func is TcIdentifier && func.symbol is SymbolFunctionName)
            // got a function call direct to a function
            TcFuncCall(location, func.type.retType, func.symbol, args, null)
        else
            TODO("Calls to calculated address")
    }

    private fun typeCheckConstructor(type:Type, args:List<TcExpr>) : TcExpr {
        if (type !is ClassType)
            return TcError(location, "Cannot call constructor for type '$type'")
        if (type.isAbstract)
            Log.error(location, "Cannot call constructor for abstract class '$type'")

        val params = type.constructorParameters
        checkArgListSymbol(location, params, args)
        return TcConstructor(location, type, args)
    }


    override fun typeCheck(context: AstBlock) : TcExpr {
        val func = func.typeCheckAllowTypeName(context)
        val args = args.map { it.typeCheck(context) }

        if (func.type is ErrorType)
            return func
        if (func.isTypeName())
            return typeCheckConstructor(func.symbol.type, args)
        if (func.type is FunctionType)
            return typeCheckFunctionCall(func, args)
        return TcError(location, "Got type '${func.type}' when expecting a function")
    }

}

class TcFuncCall (
    location: Location,
    type: Type,
    private val funcSym: SymbolFunctionName,
    private val args: List<TcExpr>,
    private val thisArg: TcExpr?
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("FUNCCALL ${funcSym.function} $type\n")
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
        if (funcSym.function.thisSymbol!=null)
            currentFunction.instrMove(allMachineRegs[argIndex++], thisReg!!)

        for (arg in args)
            currentFunction.instrMove(allMachineRegs[argIndex++], arg)

        return if (funcSym.function.methodKind == MethodKind.NONE)
            currentFunction.instrCall(funcSym.function.backendFunction)
        else
            currentFunction.instrVirtCall(thisReg!!, funcSym)
    }
}

class TcConstructor(
    location: Location,
    type: Type,
    private val args: List<TcExpr>
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("CONSTRUCTOR $type\n")
        for (arg in args) {
            arg.dump(sb, indent + 1)
        }
    }

    override fun codeGenRvalue(): Reg {
        require(type is ClassType)

        // Malloc the memory
        currentFunction.instrLea(regArg1, backend.ClassRefValue(type))
        val ret = currentFunction.instrCall(StdlibMallocObject)

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

package frontend

import backend.Reg
import backend.allMachineRegs

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
        return TcFuncCall(location, func.type.retType, func, args)
    }

    private fun typeCheckConstructor(type:Type, args:List<TcExpr>) : TcExpr {
        if (type !is ClassType)
            return TcError(location, "Cannot call constructor for type '$type'")
        if (type.definition.isAbstract)
            Log.error(location, "Cannot call constructor for abstract class '$type'")

        val params = type.definition.constructorParameters
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
    private val func: TcExpr,
    private val args: List<TcExpr>
) : TcExpr(location, type) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("FUNCCALL $type\n")
        func.dump(sb, indent + 1)
        for (arg in args) {
            arg.dump(sb, indent + 1)
        }
    }

    override fun codeGenRvalue(): Reg {
        val funcName = func.isFunctionName()
        val args = args.map { it.codeGenRvalue() }

        if (funcName!=null) {
            for (index in args.indices)
                currentFunction.instrMove(allMachineRegs[index + 1], args[index])
            return currentFunction.instrCall(funcName.function.backendFunction)
        } else {
            TODO("Function calls to calculated address")
        }
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
        TODO("Constructors")
    }
}

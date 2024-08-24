package falcon

class AstExprFuncCall (
    location: Location,
    private val func: AstExpr,
    private val args: List<AstExpr>
) : AstExpr(location) {

    var isConstructor = false

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("FUNCCALL\n")
        func.dump(sb, indent + 1)
        for (arg in args) {
            arg.dump(sb, indent + 1)
        }
    }

    override fun dumpWithType(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        if (isConstructor)
            sb.append("CONSTRUCTOR $type\n")
        else
            sb.append("FUNCCALL $type\n")
        func.dumpWithType(sb, indent + 1)
        for (arg in args) {
            arg.dumpWithType(sb, indent + 1)
        }
    }

    private fun checkArgs(params:List<Type>, args:List<AstExpr>) {
        val argTypes = args.map { it.type }
        if (params.size != argTypes.size)
            return Log.error(location, "Got ${argTypes.size} arguments when expecting ${params.size}")
        for (index in args.indices) {
            params[index].checkAssignCompatible(args[index].location, argTypes[index])
        }
    }

    private fun typeCheckFunctionCall(funcType: FunctionType) {
        checkArgs(funcType.paramTypes, args)
        type = funcType.retType
    }

    private fun typeCheckConstructor(lhs:Symbol) {
        if (lhs.type is ErrorType)
            return setTypeError()
        if (lhs.type !is ClassType)
            return setTypeError("Cannot call constructor for type '${lhs.type}'")

        val params = lhs.type.definition.params.map { it.type }
        checkArgs(params, args)
        type = lhs.type
        isConstructor = true
    }


    override fun typeCheck(context: AstBlock) {
        func.typeCheckAllowTypeName(context)
        args.forEach { it.typeCheck(context) }
        val funcType = func.type

        if (funcType is ErrorType)
            return setTypeError()
        if (func is AstExprIdentifier && func.symbol is SymbolTypeName)
            return typeCheckConstructor(func.symbol as SymbolTypeName)
        if (funcType is FunctionType)
            return typeCheckFunctionCall(funcType)
        return setTypeError("Got type '$funcType' when expecting a function")
    }

}
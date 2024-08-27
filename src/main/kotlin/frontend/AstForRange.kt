package frontend

import backend.AluOp

class AstForRange(
    location: Location,
    parent: AstBlock,
    private val name: String,
    private val start: AstExpr,
    private val end: AstExpr,
    private val inclusive: Boolean,

    ) : AstBlock(location,parent){

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("FOR_RANGE $name\n")
        start.dump(sb, indent + 1)
        end.dump(sb, indent + 1)
        for(stmt in body)
            stmt.dump(sb, indent + 1)
    }

    override fun typeCheck(context: AstBlock) : TcBlock{
        val start = start.typeCheck(context)
        val end = end.typeCheck(context)
        IntType.checkAssignCompatible(start.location, start.type)
        IntType.checkAssignCompatible(end.location, end.type)

        val symbol = SymbolLocalVar(location, name, IntType, false)
        add(symbol)

        val ret = TcForRange(location, symbolTable, symbol, start, end, inclusive)

        for(stmt in body)
            ret.add( stmt.typeCheck(this) )
        return ret
    }

}

class TcForRange(
    location: Location,
    symbolTable: SymbolTable,
    private val symbol: Symbol,
    private val start: TcExpr,
    private val end: TcExpr,
    private val inclusive: Boolean,
) : TcBlock(location,symbolTable) {

    override fun dump(sb: StringBuilder, indent: Int) {
        sb.append(". ".repeat(indent))
        sb.append("FOR_RANGE $symbol\n")
        start.dump(sb, indent + 1)
        end.dump(sb, indent + 1)
        for(stmt in body)
            stmt.dump(sb, indent + 1)
    }


    override fun codeGen() {
        val start = start.codeGenRvalue()
        val end = end.codeGenRvalue()
        val reg = currentFunction.getReg(symbol)
        currentFunction.instrMove(reg, start)
        val labelStart = currentFunction.newLabel()
        val labelCond = currentFunction.newLabel()
        currentFunction.instrJump(labelCond)
        currentFunction.instrLabel(labelStart)
        for(stmt in body)
            stmt.codeGen()

        val inc = currentFunction.instrAlu(AluOp.ADD_I, reg, 1)
        currentFunction.instrMove(reg, inc)

        currentFunction.instrLabel(labelCond)
        val op = if (inclusive) AluOp.LE_I else AluOp.LT_I
        currentFunction.instrBranch(op, reg, end, labelStart)
    }
}

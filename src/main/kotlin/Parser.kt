package falcon
import falcon.TokenKind.*

class Parser(private val lexer: Lexer) {
    private var lookahead = lexer.readToken()
    private var followingError = false

    private fun nextToken(): Token {
        val ret = lookahead
        lookahead = lexer.readToken()
        return ret
    }

    private fun expect(kind: TokenKind) : Token {
        if (lookahead.kind == kind)
            return nextToken()
        else
            throw ParseError(lookahead.location, "Got '${lookahead}' when expecting '$kind'")
    }

    private fun canTake(kind: TokenKind) : Boolean {
        if (lookahead.kind == kind) {
            nextToken()
            return true
        } else
            return false
    }

    private fun skipToEol() {
        while (lookahead.kind != EOL) {
            nextToken()
        }
        nextToken()
    }

    private fun expectEol() {
        if (lookahead.kind == EOL || lookahead.kind == EOF || lookahead.kind == SEMICOLON) {
            nextToken()
        } else {
            Log.error(lookahead.location, "Got '${lookahead}' when expecting end of line")
            skipToEol()
        }
    }

    private fun parseIdentifier() : AstExprIdentifier {
        val id = expect(ID)
        return AstExprIdentifier(id.location, id.text)
    }

    private fun parseIntLit() : AstExprIntLit {
        val lit = expect(INTLIT)
        try {
            return AstExprIntLit(lit.location, lit.text.toInt())
        } catch (e : NumberFormatException) {
            throw ParseError(lit.location, "Invalid integer literal: $lit")
        }
    }

    private fun parseStringLit(): AstExprStringLit {
        val lit = expect(STRINGLIT)
        return AstExprStringLit(lit.location, lit.text)
    }

    private fun parseBracketExpression() : AstExpr {
        expect(OPENB)
        val expr = parseExpression()
        expect(CLOSEB)
        return expr
    }

    private fun parsePrimaryExpression() : AstExpr {
        return when (lookahead.kind) {
            ID -> parseIdentifier()
            INTLIT -> parseIntLit()
            STRINGLIT -> parseStringLit()
            OPENB -> parseBracketExpression()
            else -> throw ParseError(lookahead.location, "Got '$lookahead' when expecting primary expression")
        }
    }

    private fun parseMemberAccess(lhs:AstExpr): AstExpr {
        expect(DOT)
        val id = expect(ID)
        return AstExprMemberAccess(lhs.location, lhs, AstExprIdentifier(id.location, id.text))
    }

    private fun parseFuncCall(lhs: AstExpr): AstExpr {
        expect(OPENB)
        val args = mutableListOf<AstExpr>()
        if (lookahead.kind != CLOSEB)
            do {
                args.add(parseExpression())
            } while (canTake(COMMA))
        expect(CLOSEB)
        return AstExprFuncCall(lhs.location, lhs, args)
    }

    private fun parseArrayAccess(lhs: AstExpr): AstExpr {
        expect(OPENSQ)
        val index = parseExpression()
        expect(CLOSESQ)
        return AstExprArrayAccess(lhs.location, lhs, index)
    }

    private fun parsePostfix() : AstExpr {
        var ret = parsePrimaryExpression()
        while(true)
            ret = when (lookahead.kind) {
                DOT -> parseMemberAccess(ret)
                OPENB -> parseFuncCall(ret)
                OPENSQ -> parseArrayAccess(ret)
                else -> return ret
            }
    }

    private fun parsePrefix(): AstExpr {
        var ret = parsePostfix()
        return ret
    }

    private fun parseMult() : AstExpr {
        var ret = parsePrefix()
        while(lookahead.kind in listOf(STAR, SLASH, PERCENT, AMPERSAND)) {
            val op = nextToken()
            val rhs = parsePrefix()
            ret = AstExprBinaryOp(op.location, op.kind, ret, rhs)
        }
        return ret
    }

    private fun parseAdd() : AstExpr {
        var ret = parseMult()
        while(lookahead.kind in listOf(PLUS, MINUS, BAR, CARET)) {
            val op = nextToken()
            val rhs = parseMult()
            ret = AstExprBinaryOp(op.location, op.kind, ret, rhs)
        }
        return ret
    }

    private fun parseComp() : AstExpr {
        var ret = parseAdd()
        if (lookahead.kind in listOf(EQ, NEQ, LT, GT, LTE, GTE)) {
            val op = nextToken()
            val rhs = parseAdd()
            ret = AstExprBinaryOp(op.location, op.kind, ret, rhs)
        }
        return ret
    }

    private fun parseAnd() : AstExpr {
        var ret = parseComp()
        while(lookahead.kind==AND) {
            val op = nextToken()
            val rhs = parseComp()
            ret = AstExprBinaryOp(op.location, op.kind, ret, rhs)
        }
        return ret
    }


    private fun parseOr() : AstExpr {
        var ret = parseAnd()
        while(lookahead.kind==OR) {
            val op = nextToken()
            val rhs = parseComp()
            ret = AstExprBinaryOp(op.location, op.kind, ret, rhs)
        }
        return ret
    }

    private fun parseExpression(): AstExpr {
        val expr = parseOr()
        return expr
    }

    private fun parseTypeId(): AstType {
        val id = expect(ID)
        return AstTypeId(id.location, id.text)
    }


    private fun parseTypeBracket(): AstType {
        val args = mutableListOf<AstType>()
        val loc = expect(OPENB)
        if (lookahead.kind != CLOSEB)
            do {
                args.add(parseType())
            } while (canTake(COMMA))
        expect(CLOSEB)
        if (canTake(ARROW)) {
            val retType = parseType()
            return AstTypeFunction(loc.location, args, retType)
        } else if (args.size==1)
            return args[0]
        else if (args.size>1) {
            Log.error(loc.location, "Cannot have multiple types")
            return args[0]
        } else {
            Log.error(loc.location, "Missing type")
            return AstTypeId(loc.location, "int")
        }
    }

    private fun parseTypeArray(): AstType {
        expect(ARRAY)
        expect(LT)
        val type = parseType()
        expect(GT)
        return AstTypeArray(type.location, type)
    }

    private fun parseType() : AstType {
        val ret = when(lookahead.kind) {
            ID -> parseTypeId()
            OPENB -> parseTypeBracket()
            ARRAY -> parseTypeArray()
            else -> throw ParseError(lookahead.location, "Expected type, got ${lookahead.kind}")
        }

        if (canTake(QMARK))
            return AstTypeNullable(ret.location, ret)
        return ret
    }

    private fun parseDeclaration(block: AstBlock) {
        val op = nextToken()
        val id = expect(ID)
        val type = if (canTake(COLON)) parseType() else null
        val value = if (canTake(EQ)) parseExpression() else null
        expectEol()
        block.add(AstStmtDeclaration(id.location, op.kind, id.text, type, value))
    }

    private fun parseBody(block: AstBlock) {
        while(lookahead.kind != DEDENT && lookahead.kind != EOF)
            parseStatement(block)
        expect(DEDENT)
    }

    private fun parseParameter(allowVar:Boolean) : AstParameter {
        val kind = if (allowVar && (lookahead.kind==VAR || lookahead.kind == VAL))
            nextToken().kind
        else
            EOF

        val id = expect(ID)
        expect(COLON)
        val type = parseType()
        return AstParameter(id.location, kind, id.text, type)
    }

    private fun parseParamList(allowVar: Boolean) : List<AstParameter> {
        val args = mutableListOf<AstParameter>()
        expect(OPENB)
        if (lookahead.kind != CLOSEB)
            do {
                args += parseParameter(allowVar)
            } while (canTake(COMMA))
        expect(CLOSEB)
        return args
    }

    private fun checkEnd(kind: TokenKind) {
        if (canTake(END)) {
            if (lookahead.kind != EOL)
                expect(kind)
            expectEol()
        }
    }

    private fun parseFunction(block: AstBlock) {
        val op = expect(FUN)
        val id = expect(ID)
        val args = parseParamList(false)
        val retType = if (canTake(ARROW)) parseType() else null
        expectEol()

        val ret = AstBlockFunction(id.location, block, id.text, args, retType)
        block.add(ret)

        if (canTake(INDENT))
            parseBody(ret)
        else
            Log.error(lookahead.location, "Missing function body")
        checkEnd(FUN)
    }

    private fun parseClass(block: AstBlock) {
        expect(CLASS)
        val id = expect(ID)
        val parameters = if (lookahead.kind==OPENB) parseParamList(true) else emptyList()
        expectEol()

        val ret = AstBlockClass(id.location, block, id.text, parameters)
        block.add(ret)

        if (canTake(INDENT))
            parseBody(ret)
        checkEnd(CLASS)
    }

    private fun parseReturn(block: AstBlock) {
        val op = expect(RETURN)
        val expr = if (canTake(EOL)) null else parseExpression()
        expectEol()
        block.add(AstStmtReturn(op.location, expr))
    }

    private fun parseWhile(block: AstBlock) {
        val op = expect(WHILE)
        val expr = parseExpression()
        expectEol()
        val ret = AstBlockWhile(op.location, block, expr)
        block.add(ret)

        if (canTake(INDENT))
            parseBody(ret)
        else
            Log.error(lookahead.location, "Missing while body")
        checkEnd(WHILE)
    }

    private fun parseAssign(block: AstBlock) {
        val lhs = parsePostfix()
        if (canTake(EQ)) {
            val rhs = parseExpression()
            expectEol()
            block.add(AstStmtAssign(lhs.location, lhs, rhs))
        } else {
            expectEol()
            block.add(AstStmtExpr(lhs.location, lhs))
        }
    }

    private fun parseIndent(block: AstBlock) {
        // Code to handle an unexpected indentation
        // Pretend it's a while loop,
        if (!followingError)
            Log.error(lookahead.location, "Unexpected indentation")
        expect(INDENT)
        val dummy = AstBlockWhile(lookahead.location, block, AstExprIntLit(lookahead.location, 0))
        parseBody(dummy)
        checkEnd(INDENT)
    }

    private fun parseIfClause(block: AstBlock) : AstIfClause{
        val location = lookahead.location
        canTake(ELSE)
        val expr = if (canTake(IF)) parseExpression() else null
        expectEol()
        val ret = AstIfClause(location, block, expr)
        if (canTake(INDENT))
            parseBody(ret)
        else
            Log.error(lookahead.location, "Missing if body")
        return ret
    }

    private fun parseIf(block: AstBlock) {
        val location = lookahead.location
        val clauses = mutableListOf<AstIfClause>()
        do {
            clauses += parseIfClause(block)
        } while (lookahead.kind==ELSE)

        // check that any else clauses only occur ot the end
        val elseClause = clauses.find { it.condition == null }
        if (elseClause!=null && elseClause != clauses.last())
            Log.error(elseClause.location, "Else clause must be at the end")

        block.add(AstStmtIf(location, block, clauses))
    }



    private fun parseStatement(block:AstBlock) {
        try {
            when (lookahead.kind) {
                VAR, VAL -> parseDeclaration(block)
                FUN -> parseFunction(block)
                RETURN -> parseReturn(block)
                WHILE -> parseWhile(block)
                ID, OPENB -> parseAssign(block)
                IF -> parseIf(block)
                CLASS -> parseClass(block)
                EOF -> {}
                INDENT -> parseIndent(block)
                else -> throw ParseError(lookahead.location, "Got '${lookahead}' when expecting statement")
            }

            followingError = false
        } catch (e: ParseError) {
            Log.error(e.message!!)
            skipToEol()
            followingError = true
        }
    }

    fun parseTop(top:AstBlockTop) {
        while (lookahead.kind != EOF) {
            parseStatement(top)
        }
    }

}
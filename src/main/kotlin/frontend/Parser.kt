package frontend
import frontend.TokenKind.*

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

    private fun expect(kind1: TokenKind, kind2: TokenKind) : Token {
        if (lookahead.kind == kind1 || lookahead.kind == kind2)
            return nextToken()
        else
            throw ParseError(lookahead.location, "Got '${lookahead}' when expecting '$kind1'")
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

    private fun parseIdentifier() : AstIdentifier {
        val id = expect(ID)
        return AstIdentifier(id.location, id.text)
    }

    private fun parseIntLit() : AstIntLiteral {
        val lit = expect(INTLIT)
        try {
            if (lit.text.startsWith("0x"))
                return AstIntLiteral(lit.location, IntType, lit.text.substring(2).toLong(16).toInt())
            return AstIntLiteral(lit.location, IntType, lit.text.toLong().toInt())
        } catch (e : NumberFormatException) {
            throw ParseError(lit.location, "Invalid integer literal: $lit")
        }
    }

    private fun parseStringLit(): AstStringLiteral {
        val lit = expect(STRINGLIT)
        return AstStringLiteral(lit.location, lit.text)
    }

    private fun parseCharLit() : AstIntLiteral {
        val lit = expect(CHARLIT)
        return AstIntLiteral(lit.location, CharType, lit.text[0].code)
    }

    private fun parseBracketExpression() : AstExpr {
        val location = expect(OPENB)
        val expr = parseExpression()
        if (canTake(COLON)) {
            val astType = parseType()
            expect(CLOSEB)
            return AstCast(expr.location, expr, astType)
        } else if (lookahead.kind == COMMA) {
            val exprs = mutableListOf<AstExpr>(expr)
            while (canTake(COMMA))
                exprs.add(parseExpression())
            expect(CLOSEB)
            return AstTuple(location.location, exprs)
        } else {
            expect(CLOSEB)
            return expr
        }
    }

    private fun parseArrayConstructor() : AstExpr {
        val tok = expect(MUTABLEARRAY, ARRAY)
        expect(LT)
        val astType = parseType()
        expect(GT)
        expect(OPENB)
        val size = parseExpression()
        expect(CLOSEB)
        return AstArrayConstructor(tok.location, astType, size, tok.kind==MUTABLEARRAY)
    }

    private fun parseOptGeneric() : AstType? {
        if (canTake(LT)) {
            val ret = parseType()
            expect(GT)
            return ret
        } else
            return null
    }

    private fun parseArrayOf() : AstExpr {
        val tok = expect(MUTABLEARRAYOF, ARRAYOF)
        val astType = parseOptGeneric()
        val elements = parseExpressionList()
        return AstArrayOf(tok.location, astType, elements, tok.kind == MUTABLEARRAYOF)
    }

    private fun parsePrimaryExpression() : AstExpr {
        return when (lookahead.kind) {
            ID -> parseIdentifier()
            INTLIT -> parseIntLit()
            STRINGLIT -> parseStringLit()
            CHARLIT -> parseCharLit()
            OPENB -> parseBracketExpression()
            MUTABLEARRAY, ARRAY -> parseArrayConstructor()
            MUTABLEARRAYOF, ARRAYOF -> parseArrayOf()
            else -> throw ParseError(lookahead.location, "Got '$lookahead' when expecting primary expression")
        }
    }

    private fun parseMemberAccess(lhs:AstExpr): AstExpr {
        expect(DOT)
        val id = expect(ID)
        return AstMemberAccess(id.location, lhs, id.text)
    }

    private fun parseExpressionList(): List<AstExpr> {
        expect(OPENB)
        val ret = mutableListOf<AstExpr>()
        if (lookahead.kind != CLOSEB)
            do {
                ret += parseExpression()
            } while (canTake(COMMA))
        expect(CLOSEB)
        return ret
    }

    private fun parseFuncCall(lhs: AstExpr): AstExpr {
        val args = parseExpressionList()
        return AstFuncCall(lhs.location, lhs, args)
    }

    private fun parseNullCheck(lhs:AstExpr) : AstExpr {
        val loc = expect(BANGBANG)
        return AstNullCheck(loc.location, lhs)
    }

    private fun parseArrayAccess(lhs: AstExpr): AstExpr {
        expect(OPENSQ)
        val index = parseExpression()
        expect(CLOSESQ)
        return AstArrayAccess(lhs.location, lhs, index)
    }

    private fun parsePostfix() : AstExpr {
        var ret = parsePrimaryExpression()
        while(true)
            ret = when (lookahead.kind) {
                DOT -> parseMemberAccess(ret)
                OPENB -> parseFuncCall(ret)
                OPENSQ -> parseArrayAccess(ret)
                BANGBANG -> parseNullCheck(ret)
                else -> return ret
            }
    }

    private fun parsePrefix(): AstExpr {
        if (lookahead.kind==NOT) {
            val op = nextToken()
            return AstNot(op.location, parsePrefix())
        } else if (lookahead.kind == MINUS) {
            val op = nextToken()
            return AstNeg(op.location, parsePrefix())
        } else if (lookahead.kind == TILDE) {
            val op = nextToken()
            return AstBitwiseInvert(op.location, parsePrefix())
        } else if (lookahead.kind == LOCAL) {
            val op = nextToken()
            return AstLocal(op.location, parsePrefix())
        } else
            return parsePostfix()
    }

    private fun parseMult() : AstExpr {
        var ret = parsePrefix()
        while(lookahead.kind in listOf(STAR, SLASH, PERCENT, AMPERSAND, SHL, SHR, USHR)) {
            val op = nextToken()
            val rhs = parsePrefix()
            ret = AstBinop(op.location, op.kind, ret, rhs)
        }
        return ret
    }

    private fun parseAdd() : AstExpr {
        var ret = parseMult()
        while(lookahead.kind in listOf(PLUS, MINUS, BAR, CARET)) {
            val op = nextToken()
            val rhs = parseMult()
            ret = AstBinop(op.location, op.kind, ret, rhs)
        }
        return ret
    }

    private fun parseElvis() : AstExpr {
        var ret = parseAdd()
        while (lookahead.kind == ELVIS) {
            val op = nextToken()
            val rhs = parseAdd()
            ret = AstElvis(op.location, ret, rhs)
        }
        return ret
    }

    private fun parseComp() : AstExpr {
        var ret = parseElvis()
        if (lookahead.kind in listOf(EQ, NEQ, LT, GT, LTE, GTE, IS, ISNOT)) {
            val op = nextToken()
            ret = when (op.kind) {
                EQ -> AstEquals(op.location, ret, parseElvis(), true)
                NEQ -> AstEquals(op.location, ret, parseElvis(), false)
                IS -> AstIs(op.location, ret, parseType(), true)
                ISNOT -> AstIs(op.location, ret, parseType(), false)
                else -> AstCompare(op.location, op.kind, ret, parseElvis())
            }
        }
        return ret
    }

    private fun parseAnd() : AstExpr {
        var ret = parseComp()
        while(lookahead.kind==AND) {
            val op = nextToken()
            val rhs = parseComp()
            ret = AstAnd(op.location, ret, rhs)
        }
        return ret
    }


    private fun parseOr() : AstExpr {
        var ret = parseAnd()
        while(lookahead.kind==OR) {
            val op = nextToken()
            val rhs = parseComp()
            ret = AstOr(op.location, ret, rhs)
        }
        return ret
    }

    private fun parseIfExpression() : AstExpr {
        expect(IF)
        val cond = parseExpression()
        expect(THEN)
        val thenExpr = parseExpression()
        expect(ELSE)
        val elseExpr = parseExpression()
        return AstIfExpr(cond.location, cond, thenExpr, elseExpr)
    }

    private fun parseExpression(): AstExpr {
        return when (lookahead.kind) {
            IF -> parseIfExpression()
            else -> return parseOr()
        }
    }

    private fun parseExpressionAllowTuple(): AstExpr {
        val location = lookahead.location
        val expr = parseExpression()
        if (lookahead.kind != COMMA)
            return expr

        val ret = mutableListOf<AstExpr>(expr)
        while (canTake(COMMA)){
            ret.add(parseExpression())
        }
        return AstTuple(location, ret)
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
        else if (args.size>1)
            return AstTypeTuple(loc.location, args)
        else {
            Log.error(loc.location, "Missing type")
            return AstTypeId(loc.location, "int")
        }
    }

    private fun parseTypeArray(): AstType {
        val tok = expect(MUTABLEARRAY, ARRAY)
        expect(LT)
        val type = parseType()
        expect(GT)
        return AstTypeArray(type.location, type, tok.kind == MUTABLEARRAY)
    }

    private fun parseTypePointer(): AstType {
        val tok = expect(POINTER)
        if (canTake(LT)) {
            val type = parseType()
            expect(GT)
            return AstTypePointer(tok.location, type)
        } else
            return AstTypePointer(tok.location, null)
    }

    private fun parseType() : AstType {
        val ret = when(lookahead.kind) {
            ID -> parseTypeId()
            OPENB -> parseTypeBracket()
            ARRAY, MUTABLEARRAY -> parseTypeArray()
            POINTER -> parseTypePointer()
            else -> throw ParseError(lookahead.location, "Expected type, got ${lookahead.kind}")
        }

        if (canTake(QMARK))
            return AstTypeNullable(ret.location, ret)
        return ret
    }

    private fun parseIdOrList() : List<AstIdentifier> {
        if (lookahead.kind==ID)
            return listOf(parseIdentifier())
        val ret = mutableListOf<AstIdentifier>()
        expect(OPENB)
        do {
            ret += parseIdentifier()
        } while (canTake(COMMA))
        expect(CLOSEB)
        return ret
    }

    private fun parseDeclaration(block: AstBlock) {
        val op = nextToken()
        val ids = parseIdOrList()
        val type = if (canTake(COLON)) parseType() else null
        val value = if (canTake(EQ)) parseExpression() else null
        expectEol()
        block.add(AstDeclaration(ids[0].location, op.kind, ids, type, value))
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

    private fun parseMethodKind(block:AstBlock) {
        val kind = if (canTake(OPEN))   MethodKind.OPEN_METHOD
        else if (canTake(OVERRIDE))     MethodKind.OVERRIDE_METHOD
        else if (canTake(ABSTRACT))     MethodKind.ABSTRACT_METHOD
        else                            MethodKind.NONE

        return when (lookahead.kind) {
            FUN -> parseFunction(block, kind)
            CLASS -> parseClass(block, kind)
            else -> throw ParseError(lookahead.location, "Expected function or class, got ${lookahead.kind}")
        }
    }

    private fun parseFunction(block: AstBlock, methodKind: MethodKind) {
        val methodOf = if (block is AstClass) block else null
        if (methodOf==null && methodKind!= MethodKind.NONE)
            Log.error(lookahead.location, "Cannot have method outside class")

        val external = canTake(EXTERNAL)
        val tok = expect(FUN)
        val id = expect(ID, PRINT)
        val args = parseParamList(false)
        val retType = if (canTake(ARROW)) parseType() else null
        expectEol()

        val ret = AstFunction(id.location, block, id.text, args, retType, methodKind, methodOf, external)
        block.add(ret)

        if (methodKind==MethodKind.ABSTRACT_METHOD) {
            if (methodOf!=null && !methodOf.isAbstract)
                Log.error(tok.location, "Cannot have abstract method outside abstract class")
            // Abstract methods are not allowed to have a body
            if (canTake(INDENT)) {
                Log.error(lookahead.location, "Abstract methods cannot have a body")
                parseBody(ret)
            }
        } else if (canTake(INDENT))
            parseBody(ret)
        else
            Log.error(lookahead.location, "Missing function body")

        ret.endLocation = lookahead.location
        checkEnd(FUN)
    }

    private fun parseClass(block: AstBlock, methodKind: MethodKind) {
        if (methodKind!=MethodKind.NONE && methodKind!=MethodKind.ABSTRACT_METHOD)
            Log.error(lookahead.location, "$methodKind cannot be appleid to class")

        expect(CLASS)
        val id = expect(ID)
        val parameters = if (lookahead.kind==OPENB) parseParamList(true) else emptyList()
        val superClassName : AstIdentifier?
        val superClassConstuctorArgs : List<AstExpr>
        if (canTake(COLON)) {
            superClassName = parseIdentifier()
            superClassConstuctorArgs = parseExpressionList()
        } else {
            superClassName = null
            superClassConstuctorArgs = emptyList()
        }
        expectEol()

        val isAbstract = methodKind == MethodKind.ABSTRACT_METHOD
        val ret = AstClass(id.location, block, id.text, parameters, superClassName, superClassConstuctorArgs, isAbstract)
        block.add(ret)

        if (canTake(INDENT))
            parseBody(ret)
        checkEnd(CLASS)
    }

    private fun parseReturn(block: AstBlock) {
        val op = expect(RETURN)
        val expr = if (lookahead.kind==EOL) null else parseExpressionAllowTuple()
        expectEol()
        block.add(AstReturn(op.location, expr))
    }

    private fun parseWhile(block: AstBlock) {
        val op = expect(WHILE)
        val expr = parseExpression()
        expectEol()
        val ret = AstWhile(op.location, block, expr)
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
            block.add(AstAssign(lhs.location, lhs, rhs))
        } else {
            expectEol()
            block.add(AstExprStmt(lhs.location, lhs))
        }
    }

    private fun parseConst(block: AstBlock) {
        expect(CONST)
        val id = expect(ID)
        expect(EQ)
        val expr = parseExpression()
        expectEol()

        block.add(AstConst(id.location, id.text, expr))
    }

    private fun parseIndent(block: AstBlock) {
        // Code to handle an unexpected indentation
        // Pretend it's a while loop,
        if (!followingError)
            Log.error(lookahead.location, "Unexpected indentation")
        expect(INDENT)
        val dummy = AstWhile(lookahead.location, block, AstIntLiteral(lookahead.location, IntType, 0))
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
        checkEnd(IF)

        // check that any else clauses only occur ot the end
        val elseClause = clauses.find { it.condition == null }
        if (elseClause!=null && elseClause != clauses.last())
            Log.error(elseClause.location, "Else clause must be at the end")

        block.add(AstIf(location, block, clauses))
    }

    private fun parseIdList() : List<AstIdentifier> {
        val ret = mutableListOf<AstIdentifier>()
        expect(OPENB)
        if (lookahead.kind != CLOSEB)
            do {
                ret += parseIdentifier()
            } while (canTake(COMMA))
        expect(CLOSEB)
        return ret
    }

    private fun parseEnum(block: AstBlock) {
        expect(ENUM)
        val id = expect(ID)
        val values = parseIdList()
        expectEol()
        val ret = AstEnum(id.location, block, id.text, values)
        block.add(ret)
    }

    private fun parseRepeat(block: AstBlock) {
        expect(REPEAT)
        expectEol()

        val ret = AstRepeat(lookahead.location, block)
        block.add(ret)

        if (canTake(INDENT))
            parseBody(ret)
        else
            Log.error(lookahead.location, "Missing while body")

        expect(UNTIL)
        ret.condition = parseExpression()
        expectEol()
    }

    private fun parsePrint(block:AstBlock) {
        val tok = nextToken()
        val exprs = mutableListOf<AstExpr>()
        do {
            exprs += parseExpression()
        } while (canTake(COMMA))
        expectEol()
        val newline = tok.kind == PRINTLN || tok.kind == PRINTHEXLN
        val hexFormat = tok.kind == PRINTHEX || tok.kind == PRINTHEXLN
        block.add(AstPrint(tok.location,exprs, newline, hexFormat))
    }

    private fun parseFor(block: AstBlock) {
        expect(FOR)
        val id = expect(ID)
        expect(IN)
        val start = parseExpression()
        expect(TO)
        val inclusive = !canTake(LT)
        val end = parseExpression()
        expectEol()
        val ret = AstForRange(id.location, block, id.text, start, end, inclusive)

        if (canTake(INDENT))
            parseBody(ret)
        else
            Log.error(lookahead.location, "Missing for loop body")

        checkEnd(FOR)
        block.add(ret)
    }

    private fun parseStatement(block:AstBlock) {
        try {
            when (lookahead.kind) {
                VAR, VAL -> parseDeclaration(block)
                OPEN, OVERRIDE, ABSTRACT -> parseMethodKind(block)
                EXTERNAL, FUN -> parseFunction(block, MethodKind.NONE)
                RETURN -> parseReturn(block)
                WHILE -> parseWhile(block)
                ID, OPENB -> parseAssign(block)
                IF -> parseIf(block)
                CLASS -> parseClass(block, MethodKind.NONE)
                ENUM -> parseEnum(block)
                REPEAT -> parseRepeat(block)
                EOF -> {}
                INDENT -> parseIndent(block)
                PRINT, PRINTLN, PRINTHEX, PRINTHEXLN -> parsePrint(block)
                FOR -> parseFor(block)
                CONST -> parseConst(block)
                else -> throw ParseError(lookahead.location, "Got '${lookahead}' when expecting statement")
            }

            followingError = false
        } catch (e: ParseError) {
            Log.error(e.message!!)
            skipToEol()
            followingError = true
        }
    }

    fun parseTop(top:AstTop) {
        while (lookahead.kind != EOF) {
            parseStatement(top)
        }
    }

}
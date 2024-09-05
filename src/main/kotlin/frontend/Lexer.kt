package frontend

import frontend.TokenKind.*
import java.io.Reader

class Lexer (val fineName:String, val reader: Reader) {
    private var lineNumber = 1
    private var columnNumber = 1
    private var atEof = false
    private var lookahead = readChar()
    private var lineContinues = true
    private var atStartOfLine = true
    private val indentStack = mutableListOf(1)

    private fun readChar(): Char {
        val c = reader.read()
        return if (c == -1) {
            atEof = true
            '\n'
        } else
            c.toChar()
    }

    private fun nextChar() : Char {
        val c = lookahead
        lookahead = readChar()
        if (c=='\n') {
            lineNumber++
            columnNumber = 1
        } else
            columnNumber++

        return c
    }

    private fun skipWhitespaceAndComments() {
        while (lookahead == ' ' || lookahead == '\t' || lookahead=='\r' || lookahead == '#' ||
            (lookahead == '\n' && lineContinues && !atEof)) {
            if (lookahead == '#')
                while (lookahead != '\n' && !atEof)
                    nextChar()
            else
                nextChar()
        }
    }

    private fun readWord() : String {
        val sb = StringBuilder()
        while (lookahead.isJavaIdentifierPart() || lookahead == '_' || lookahead=='@')
            sb.append(nextChar())
        return sb.toString()
    }

    private fun readEscapedChar() : Char {
        val c = nextChar()
        return if (c=='\\')
            when(val c2 = nextChar()) {
            'n' -> '\n'
            't' -> '\t'
            '"' -> '"'
            '\\' -> '\\'
            '\'' -> '\''
            else -> c2
        } else
            c
    }

    private fun readString() : String {
        nextChar()  // skip opening quote
        val sb = StringBuilder()
        while (lookahead != '"' && !atEof)
            sb.append(readEscapedChar())
        if (lookahead== '"')
            nextChar()  // skip closing quote
        else
            Log.error(Location(fineName, lineNumber, columnNumber), "Unterminated string literal")
        return sb.toString()
    }

    private fun readCharLiteral(): String {
        val sb = StringBuilder()
        nextChar() // skip opening quote
        while (lookahead != '\'' && !atEof)
            sb.append(readEscapedChar())
        if (lookahead == '\'')
            nextChar()  // skip closing quote
        else
            Log.error(Location(fineName, lineNumber, columnNumber), "Unterminated character literal")
        return sb.toString()
    }

    private fun readPunctuation() : String {
        val c = nextChar()
        return if ((c == '<' && lookahead == '=') ||
            (c == '>' && lookahead == '=') ||
            (c == '!' && lookahead == '=') ||
            (c == '-' && lookahead == '>') ||
            (c == '?' && lookahead == ':') ||
            (c == '?' && lookahead == '.') )
            c.toString() + nextChar()
        else
            c.toString()
    }

    fun readToken(): Token {
        skipWhitespaceAndComments()
        val startLine = lineNumber
        val startColumn = columnNumber
        val kind: TokenKind
        val text: String

        if (atEof) {
            if (!atStartOfLine) {
                kind = EOL
            } else if (indentStack.size > 1) {
                kind = DEDENT
                indentStack.removeLast()
            } else {
                kind = EOF
            }
            text = kind.text

        } else if (atStartOfLine && columnNumber> indentStack.last()) {
            kind = INDENT
            indentStack.add(columnNumber)
            text = kind.text

        } else if (atStartOfLine && columnNumber < indentStack.last()) {
            kind = DEDENT
            if (indentStack.size>1)
                indentStack.removeLast()
            if (columnNumber> indentStack.last())
                Log.error(Location(fineName, lineNumber, columnNumber), "Indentation error - expected column ${indentStack.last()}")
            text = kind.text

        } else if (lookahead == '\n') {
            kind = EOL
            text = kind.text
            nextChar()

        } else if (lookahead.isLetter() || lookahead == '_' || lookahead=='@') {
            text = readWord()
            kind = TokenKind.map.getOrDefault(text, ID)

        } else if (lookahead.isDigit()) {
            val word = readWord()
            if (lookahead == '.') {
                nextChar()
                text = word + '.' + readWord()
                kind = REALLIT
            } else {
                text = word
                kind = INTLIT
            }

        } else if (lookahead == '"') {
            text = readString()
            kind = STRINGLIT

        } else if (lookahead == '\'') {
            text = readCharLiteral()
            kind = CHARLIT

        } else {
            text = readPunctuation()
            kind = TokenKind.map.getOrDefault(text, ERROR)
            if (kind==ERROR)
                Log.error(Location(fineName, lineNumber, columnNumber), "Unknown punctuation: $text")
        }

        atStartOfLine = (kind == EOL) || (kind == DEDENT)
        lineContinues = kind.lineContinues
        val ret = Token(Location(fineName, startLine, startColumn), kind, text)
//        println("Token: $ret")
        return ret
    }
}
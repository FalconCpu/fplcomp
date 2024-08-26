import frontend.Lexer
import frontend.Token
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader
import frontend.TokenKind.*

class LexerTest {

    private fun lexInput(input: String): List<Token> {
        val reader = StringReader(input)
        val lexer = Lexer("test_input", reader)
        val tokens = mutableListOf<Token>()

        do {
            val token = lexer.readToken()
            tokens.add(token)
        } while (token.kind != EOF)

        return tokens
    }

    @Test
    fun testSimpleIndentation() {
        val input = """
        var x = 10
        if x >= 5
            x = x + 1
        end
    """.trimIndent()

        val tokens = lexInput(input)

        val expectedKinds = listOf(
            VAR, ID, EQ, INTLIT, EOL,
            IF, ID, GTE, INTLIT, EOL,
            INDENT, ID, EQ, ID, PLUS, INTLIT, EOL,
            DEDENT, END, EOL, EOF
        )

        val actualKinds = tokens.map { it.kind }

        assertEquals(expectedKinds, actualKinds)
    }

    @Test
    fun testStringLiteral() {
        val input = """
            val greeting = "Hello, world!"
        """.trimIndent()

        val tokens = lexInput(input)

        val expectedKinds = listOf(
            VAL, ID, EQ, STRINGLIT, EOL, EOF
        )

        val actualKinds = tokens.map { it.kind }

        assertEquals(expectedKinds, actualKinds)
        assertEquals("Hello, world!", tokens[3].text)  // Check the content of the string literal
    }

    @Test
    fun testIndentationError() {
        val input = """
            var x = 10
            if x > 5     # comment
                x = x + 1
              end
        """.trimIndent()

        val expectedError = """
            test_input 4.3:- Indentation error - expected column 1
        """.trimIndent()

        Log.initialize()
        lexInput(input)
        assertEquals(expectedError, Log.dump())
    }

    @Test
    fun testUnterminatedString() {
        val input = """
            val greeting = "Hello, world!
        """.trimIndent()

        val expectedError = """
            test_input 1.30:- Unterminated string literal
        """.trimIndent()

        Log.initialize()
        lexInput(input)
        assertEquals(expectedError, Log.dump())
    }

    @Test
    fun testUnterminatedChar() {
        val input = """
            val greeting = 'g
        """.trimIndent()

        val expectedError = """
            test_input 1.18:- Unterminated character literal
        """.trimIndent()

        Log.initialize()
        lexInput(input)
        assertEquals(expectedError, Log.dump())
    }


    @Test
    fun testCharLiteral() {
        val input = """
            val 'c' '\n' '\t' '\'' '\"' '\x'
        """.trimIndent()

        val tokens = lexInput(input)

        val expectedKinds = listOf(
            VAL, CHARLIT, CHARLIT, CHARLIT, CHARLIT, CHARLIT, CHARLIT, EOL, EOF
        )

        val actualKinds = tokens.map { it.kind }

        assertEquals(expectedKinds, actualKinds)
        assertEquals("c", tokens[1].text)
        assertEquals("\n", tokens[2].text)
        assertEquals("\t", tokens[3].text)
        assertEquals("\'", tokens[4].text)
        assertEquals("\"", tokens[5].text)
        assertEquals("x", tokens[6].text)
    }

    @Test
    fun testRealLiteral() {
        val input = """
            val x = 1.3
        """.trimIndent()

        val tokens = lexInput(input)

        val expectedKinds = listOf(
            VAL, ID, EQ, REALLIT, EOL, EOF
        )

        val actualKinds = tokens.map { it.kind }

        assertEquals(expectedKinds, actualKinds)
        assertEquals("1.3", tokens[3].text)
    }


    @Test
    fun unknownPunctuationError() {
        val input = """
            var £
        """.trimIndent()

        val expectedError = """
            test_input 1.6:- Unknown punctuation: £
        """.trimIndent()

        Log.initialize()
        lexInput(input)
        assertEquals(expectedError, Log.dump())
    }


}
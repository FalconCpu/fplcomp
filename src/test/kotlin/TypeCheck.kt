package falcon

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class TypeCheck {
    private fun runTest(prog: String, expected: String) {
        val lexer = Lexer("test.txt", StringReader(prog))
        val output = compile(listOf(lexer), StopAt.TYPECHECK)
        assertEquals(expected, output)
    }

    @Test
    fun simpleDeclaration() {
        val prog = """
            val a = 1
            val b = a
            var c = b + 1
        """.trimIndent()

        val expected = """
            TOP
            . ASSIGN
            . . GLOBALVAR a Int
            . . LITERAL 1 Int
            . ASSIGN
            . . GLOBALVAR b Int
            . . GLOBALVAR a Int
            . ASSIGN
            . . GLOBALVAR c Int
            . . BINOP ADD_I Int
            . . . GLOBALVAR b Int
            . . . LITERAL 1 Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun duplicateDeclaration() {
        val prog = """
            val a = 1
            val a = 2
        """.trimIndent()

        val expected = """
            test.txt 2.5:- duplicate symbol: a
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun undefinedType() {
        val prog = """
            val a
        """.trimIndent()

        val expected = """
            test.txt 1.5:- Unknown type for a
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun declBadType() {
        val prog = """
            val a : Int = "hello"
        """.trimIndent()

        val expected = """
            test.txt 1.15:- Got type String when expecting Int
        """.trimIndent()

        runTest(prog, expected)
    }



    fun emptyTest() {
        val prog = """

        """.trimIndent()

        val expected = """
            """.trimIndent()

        runTest(prog, expected)
    }
}
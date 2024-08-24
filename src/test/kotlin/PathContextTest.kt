import falcon.Lexer
import falcon.StopAt
import falcon.compile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class PathContextTest {

    private fun runTest(prog: String, expected: String) {
        val lexer = Lexer("test.txt", StringReader(prog))
        val output = compile(listOf(lexer), StopAt.TYPECHECK)
        assertEquals(expected, output)
    }

    @Test
    fun uninitializedVariable() {
        val prog = """
            fun main()->Int
                val x : Int
                return x
        """.trimIndent()

        val expected = """
            test.txt 3.12:- Variable 'x' has not been initialized
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun initializingVal() {
        val prog = """
            fun main()->Int
                val x : Int
                x = 5
                return x
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main ()->Int
            . . DECL LOCALVAR x Int
            . . ASSIGN
            . . . IDENTIFIER LOCALVAR x Int
            . . . INTLIT 5 Int
            . . RETURN
            . . . IDENTIFIER LOCALVAR x Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun assigningVal() {
        val prog = """
            fun main()->Int
                val x : Int
                x = 5
                x = 6      # error as reassigning val
                return x
        """.trimIndent()

        val expected = """
            test.txt 4.5:- Local variable x is not mutable
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun assigningVar() {
        val prog = """
            fun main()->Int
                var x : Int
                x = 5
                x = 6      # no error as reassigning var
                return x
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main ()->Int
            . . DECL LOCALVAR x Int
            . . ASSIGN
            . . . IDENTIFIER LOCALVAR x Int
            . . . INTLIT 5 Int
            . . ASSIGN
            . . . IDENTIFIER LOCALVAR x Int
            . . . INTLIT 6 Int
            . . RETURN
            . . . IDENTIFIER LOCALVAR x Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun maybeUninitialized() {
        val prog = """
            fun main(a:Int)->Int
                val x : Int
                if a=0
                    x = 5
                return x     # error as x may not be initialized
        """.trimIndent()

        val expected = """
            test.txt 5.12:- Variable 'x' might not be initialized
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun maybeUninitialized2() {
        val prog = """
            fun main(a:Int)->Int
                val x : Int
                if a=0
                    x = 5
                x = 6      # error as reassigning val
                return x
        """.trimIndent()

        val expected = """
            test.txt 5.5:- Local variable x is not mutable
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun returnMakesUnreachable() {
        val prog = """
            fun main(a:Int)->Int
                val x : Int
                if a=0
                    x = 5
                    return x
                x = 6      # No error as x is still uninitialized if code reaches here
                return x
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main (Int)->Int
            . . DECL LOCALVAR x Int
            . . IF
            . . . CLAUSE
            . . . . BINARYOP = Bool
            . . . . . IDENTIFIER LOCALVAR a Int
            . . . . . INTLIT 0 Int
            . . . . ASSIGN
            . . . . . IDENTIFIER LOCALVAR x Int
            . . . . . INTLIT 5 Int
            . . . . RETURN
            . . . . . IDENTIFIER LOCALVAR x Int
            . . ASSIGN
            . . . IDENTIFIER LOCALVAR x Int
            . . . INTLIT 6 Int
            . . RETURN
            . . . IDENTIFIER LOCALVAR x Int

        """.trimIndent()

        runTest(prog, expected)
    }



    @Test
    fun initializedInBothBranches() {
        val prog = """
            fun main(a:Int)->Int
                val x : Int
                if a=0
                    x = 5
                else
                    x = 6
                return x     # OK - as initialized in both branches
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main (Int)->Int
            . . DECL LOCALVAR x Int
            . . IF
            . . . CLAUSE
            . . . . BINARYOP = Bool
            . . . . . IDENTIFIER LOCALVAR a Int
            . . . . . INTLIT 0 Int
            . . . . ASSIGN
            . . . . . IDENTIFIER LOCALVAR x Int
            . . . . . INTLIT 5 Int
            . . . CLAUSE
            . . . . ASSIGN
            . . . . . IDENTIFIER LOCALVAR x Int
            . . . . . INTLIT 6 Int
            . . RETURN
            . . . IDENTIFIER LOCALVAR x Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun missingReturnInSomePath() {
        val prog = """
            fun main(a:Int)->Int
                if a=0
                    return 5
                # should be an error here as code can fall off the end without returning Int
        """.trimIndent()

        val expected = """
            test.txt 4.81:- Function should return a value of type Int
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun returnInBothPaths() {
        val prog = """
            fun main(a:Int)->Int
                if a=0
                    return 5
                else
                    return 6    
                # No error here as both paths return Int
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main (Int)->Int
            . . IF
            . . . CLAUSE
            . . . . BINARYOP = Bool
            . . . . . IDENTIFIER LOCALVAR a Int
            . . . . . INTLIT 0 Int
            . . . . RETURN
            . . . . . INTLIT 5 Int
            . . . CLAUSE
            . . . . RETURN
            . . . . . INTLIT 6 Int

        """.trimIndent()

        runTest(prog, expected)
    }




}
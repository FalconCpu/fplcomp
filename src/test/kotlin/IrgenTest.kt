import frontend.Lexer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class IrgenTest {
    private fun runTest(prog: String, expected: String) {
        val lexer = Lexer("test.txt", StringReader(prog))
        val output = compile(listOf(lexer), StopAt.IRGEN)
        assertEquals(expected, output)
    }

    @Test
    fun simpleDeclaration() {
        val prog = """
            fun main()
                val x = 10           
        """.trimIndent()

        val expected = """
            Function <top>
            start
            call main
            end

            Function main
            start
            t0 = 10
            x = t0
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun simpleArithmetic() {
        val prog = """
            fun main()
                val x = 10
                val y = x + 5
        """.trimIndent()

        val expected = """
            Function <top>
            start
            call main
            end

            Function main
            start
            t0 = 10
            x = t0
            t1 = 5
            t2 = x + t1
            y = t2
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun whileLoop() {
        val prog = """
            fun main()
                var x = 0
                while x < 10
                    x = x + 1
        """.trimIndent()

        val expected = """
            Function <top>
            start
            call main
            end

            Function main
            start
            t0 = 0
            x = t0
            jmp @2
            @1:
            t1 = 1
            t2 = x + t1
            x = t2
            @2:
            t3 = 10
            if x < t3 jmp @1
            jmp @3
            @3:
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun helloWorld() {
        val prog = """
            fun main()
                println "Hello, world!", 42
        """.trimIndent()

        val expected = """
            Function <top>
            start
            call main
            end

            Function main
            start
            t0 = ADDR("Hello, world!")
            %1 = t0
            call StdlibPrintString
            t1 = 42
            %1 = t1
            call StdlibPrintInt
            call StdlibNewline
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }



}
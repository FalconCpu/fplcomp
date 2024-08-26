import backend.runInterpreter
import frontend.Lexer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

private val debug = true

class InterpreterTest {

    private fun runTest(prog: String, expected: String) {
        val lexer = Lexer("test.txt", StringReader(prog))
        val prog = compile(listOf(lexer), StopAt.IRGEN)

        if (Log.anyError())
            error(Log.dump())

        if (debug)
            println(prog)

        val output = runInterpreter()
        assertEquals(expected, output)
    }

    @Test
    fun helloWorld() {
        val prog = """
            fun main()
                println "Hello, world!", 42
        """.trimIndent()

        val expected = """
            Hello, world!42
            
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun arithOperation() {
        val prog = """
            fun main()
                val a = 10
                val b = 5
            
                # Addition
                val add = a + b
                println "Addition result: ", add
            
                # Subtraction
                val sub = a - b
                println "Subtraction result: ", sub
            
                # Multiplication
                val mul = a * b
                println "Multiplication result: ", mul
            
                # Division
                val div = a / b
                println "Division result: ", div
            
                # Modulus
                val mod = a % b
                println "Modulus result: ", mod
            
                # Complex expression
                val complex = (a + b) * (a - b) / (b + 1)
                println "Complex expression result: ", complex
        """.trimIndent()

        val expected = """
            Addition result: 15
            Subtraction result: 5
            Multiplication result: 50
            Division result: 2
            Modulus result: 0
            Complex expression result: 12

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun whileLoop() {
        val prog = """
            fun main()
                var i = 0
                while i < 5
                    println "Current value of i: ", i
                    i = i + 1
                println "Loop finished."
        """.trimIndent()

        val expected = """
            Current value of i: 0
            Current value of i: 1
            Current value of i: 2
            Current value of i: 3
            Current value of i: 4
            Loop finished.

        """.trimIndent()

        runTest(prog, expected)
    }

}
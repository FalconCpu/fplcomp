import frontend.Lexer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class RegallocTest {
    private fun runTest(prog: String, expected: String) {
        val lexer = Lexer("test.txt", StringReader(prog))
        val output = compile(listOf(lexer), StopAt.REGALLOC)
        assertEquals(expected, output)
    }

    @Test
    fun sumArray() {
        val prog = """
            fun main(a: Array<Int>)->Int
                var sum = 0
                for i in 0 to <a.size
                    sum = sum + a[i]
                return sum           
        """.trimIndent()

        val expected = """
        """.trimIndent()

        runTest(prog, expected)
    }
}


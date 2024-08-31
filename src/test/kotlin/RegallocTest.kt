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
            Function <top>
            start
            call main(Array<Int>)
            end

            Function main(Array<Int>)
            start
            mov %8, 0
            ldw %2, %1->size
            mov %3, 0
            jmp @2
            @1:
            ldw %4, %1[%3]
            add %8, %8, %4
            add %3, %3, 1
            @2:
            blt, %3, %2, @1
            end


        """.trimIndent()

        runTest(prog, expected)
    }
}


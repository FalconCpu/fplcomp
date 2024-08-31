import frontend.Lexer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class PolymorphTest {

    private fun runTest(prog: String, expected: String) {
        val lexer = Lexer("test.txt", StringReader(prog))
        val output = compile(listOf(lexer), StopAt.IRGEN)
        assertEquals(expected, output)
    }

    @Test
    fun polymorphTest() {
        val prog = """
            fun add(a: Int, b: Int)->Int
                return a + b
            
            fun add(a:String, b:String)->String
                return a + b
                
            fun main()
                println add("Hello", "World")
                println add(1, 2)
        """.trimIndent()

        val expected = """
            Function <top>
            start
            call main()
            end

            Function add(Int,Int)
            start
            mov a, %1
            mov b, %2
            add t0, a, b
            mov %8, t0
            jmp @0
            @0:
            end

            Function add(String,String)
            start
            mov a, %1
            mov b, %2
            mov %1, a
            mov %2, b
            call StdlibStrcat
            mov t0, %8
            mov %8, t0
            jmp @0
            @0:
            end

            Function main()
            start
            lea t0, "Hello"
            lea t1, "World"
            mov %1, t0
            mov %2, t1
            call add(String,String)
            mov t2, %8
            mov %1, t2
            call StdlibPrintString
            call StdlibNewline
            mov t3, 1
            mov t4, 2
            mov %1, t3
            mov %2, t4
            call add(Int,Int)
            mov t5, %8
            mov %1, t5
            call StdlibPrintInt
            call StdlibNewline
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }
}


import frontend.Lexer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class AsmGenTest {

    private fun runTest(prog: String, expected: String) {
        val lexer = Lexer("test.txt", StringReader(prog))
        val prog = compile(listOf(lexer), StopAt.ASMGEN)

        if (Log.anyError())
            error(Log.dump())

        assertEquals(expected, prog)
    }


    @Test
    fun helloWorld() {
        val prog = """
            fun main()
                println "Hello, world!", 42
        """.trimIndent()


        val expected = """
            <top>:
            jsr main()
            ret

            main():
            ld %1, "Hello, world!"
            jsr StdlibPrintString
            ld %1, 42
            jsr StdlibPrintInt
            jsr StdlibNewline
            ret


        """.trimIndent()


        runTest(prog, expected)
    }

}
import frontend.Lexer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.FileReader
import java.io.FileWriter
import java.io.StringReader

class StdlibTest {

    val stdLibFiles = listOf("hwregs.fpl", "Memory.fpl", "Print.fpl")


    private fun runTest(prog: String, expected: String) {
        val lexers = stdLibFiles.map { Lexer(it , FileReader("src/main/stdlib/$it")) }
        val lexer2 = Lexer("test.txt", StringReader(prog))
        val prog = compile(lexers + lexer2, StopAt.ASMGEN)

        if (Log.anyError())
            error(Log.dump())

        val outfile = FileWriter("out.f32")
        outfile.write(prog)
        outfile.close()

        val assemblerOut = "f32asm.exe out.f32".runCommand()
        if (assemblerOut!="")
            error(assemblerOut!!)

        val emulatorOut = "f32sim.exe rom.hex".runCommand()

        assertEquals(expected, emulatorOut)
    }


    @Test
    fun helloWorld() {
        val prog = """
            fun main()
                printHex(0)
                uart_transmit(10)
                printHex(100)
                uart_transmit(10)
                printHex(-100)
                uart_transmit(10)
        """.trimIndent()


        val expected = """
            """.trimIndent()


        runTest(prog, expected)
    }

}
import frontend.Lexer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.FileReader
import java.io.FileWriter
import java.io.StringReader

class OsTest {

    val stdLibFiles = listOf(
        "hwregs.fpl", "Fatal.fpl", "Memory.fpl", "String.fpl", "Print.fpl",
        "Keyboard.fpl", "Graphics.fpl", "StringBuffer.fpl", "LineEditor.fpl"
    )

    private fun runTest(prog: String, expected: String) {
        val lexers = stdLibFiles.map { Lexer(it, FileReader("src/main/stdlib/$it")) }
        val lexer2 = Lexer("test.txt", StringReader(prog))
        val prog = compile(lexers + lexer2, StopAt.ASMGEN)

        if (Log.anyError())
            error(Log.dump())

        val outfile = FileWriter("out.f32")
        outfile.write(prog)
        outfile.close()

        val assemblerOut = "f32asm.exe out.f32".runCommand()
        if (assemblerOut != "")
            error(assemblerOut!!)

        val emulatorOut = "f32sim.exe -d rom.hex".runCommand()

        val output = emulatorOut?.replace("\r\n", "\n")
        assertEquals(expected, output)
    }

    @Test
    fun lineEditorTest() {
        val prog = """
            fun main()
                outputChannel = PrintChannel.SCREEN
                val le = LineEditor()
                clearScreen()

                while(true)
                    printPrompt()
                    val cmd = le.getLine()
                    setFgColor(8)
                    println ""
                    processCommand(cmd)
                    
                    
            fun printPrompt() 
                setFgColor(14)
                print "\n> "
                setFgColor(15)
            
            fun processCommand(cmd: String)
                if cmd = "cls"
                    clearScreen()
                else if cmd = "version"
                    println("Version 0.0.1")
                else if cmd = "dumpMemory"
                    dumpMemory()
                else
                    println("Unknown command")
            
            """.trimIndent()


        val expected = """
            Timeout
            
        """.trimIndent()

        runTest(prog, expected)

    }

}
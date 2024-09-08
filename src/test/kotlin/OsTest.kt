import frontend.Lexer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.FileReader
import java.io.FileWriter
import java.io.StringReader

class OsTest {

    val stdLibFiles = listOf(
        "hwregs.fpl", "Fatal.fpl", "Memory.fpl", "String.fpl", "Print.fpl",
        "Keyboard.fpl", "Graphics.fpl", "StringBuffer.fpl", "LineEditor.fpl", "List.fpl")

    val osFiles = listOf("BlockDevice.fpl", "DiskBuffer.fpl", "FileSystem.fpl", "Inode.fpl",
        "DirectoryHandle.fpl", "FileHandle.fpl", "main.fpl")

    private fun runTest() {
        val lexers = stdLibFiles.map { Lexer(it, FileReader("src/fpl/stdlib/$it")) }
        val lexers2 = osFiles.map { Lexer(it, FileReader("src/fpl/fileSystem/$it")) }
        val prog = compile(lexers + lexers2, StopAt.ASMGEN, forFPGA = true)

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
        println("Emulator output:")
        println(output)
    }

    @Test
    fun osTest() {
        runTest()
    }
}
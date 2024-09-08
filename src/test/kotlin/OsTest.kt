import frontend.Lexer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.FileReader
import java.io.FileWriter
import java.io.StringReader

class OsTest {

    val stdLibFiles = listOf(
        "hwregs.fpl", "Fatal.fpl", "Memory.fpl", "String.fpl", "Print.fpl",
        "Keyboard.fpl", "Graphics.fpl", "StringBuffer.fpl", "LineEditor.fpl")

    val osFiles = listOf("BlockDevice.fpl", "DiskBuffer.fpl", "FileSystem.fpl", "Inode.fpl",
        "DirectoryHandle.fpl", "FileHandle.fpl")

    private fun runTest(prog: String) {
        val lexers = stdLibFiles.map { Lexer(it, FileReader("src/fpl/stdlib/$it")) }
        val lexers2 = osFiles.map { Lexer(it, FileReader("src/fpl/fileSystem/$it")) }
        val lexer3 = Lexer("test.txt", StringReader(prog))
        val prog = compile(lexers + lexers2 + lexer3, StopAt.ASMGEN)

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
        assertEquals(1,2)
    }

    @Test
    fun osTest() {
        val prog = """
            val blockDevice = BlockDevice("dh0")
            val fileSystem = FileSystem(blockDevice)
            var currentDirectory = fileSystem.getRootDirectory()!!
            
            fun main()
                outputChannel = PrintChannel.SCREEN
                val le = LineEditor()
                fileSystem.allocateBuffers()
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
            
            fun writeBlock()
                val block = MutableArray<Int>(128)
                for i in 0 to <128
                    block[i] = i*5
                blockDevice.writeBlock(13, block)
     
            fun readBlock() 
                val buffer = MutableArray<Char>(20000)
                val blk = fileSystem.openSector(9)
                if blk = null
                    kprint("Cannot open sector")
                    return
                val inode = Inode(fileSystem, blk)
                val length = inode.read(0, 20000, (buffer:Pointer))
                kprint("length = ")
                kprint(length)
                
                for i in 0 to <length
                    print buffer[i]
            
            fun cmdLs()
                currentDirectory.beginDirectoryScan()
                while currentDirectory.nextDirectorScan()
                    kprint("CCCC\n")
                    fwprint(currentDirectory.name, 40)
                    if currentDirectory.isDirectory
                        print "   Dir"
                    else
                        fwprint(currentDirectory.fileLength,6)
                    print " "
                    println currentDirectory.inodeNumber
                kprint("BBBBB\n")
                
            fun cmdCat() 
                kprint("1\n")
                val fh = currentDirectory.openFile("mary.txt")
                if fh = null
                    println("Cannot open file")
                    return 
                
                kprint("2\n")
                val buffer = makeString(1024)
                
                while(true)
                    kprint("3\n")
                    val length = fh.read((buffer:Pointer), 1024)
                    if length = 0
                        kprint("4\n")
                        # free(buffer)
                        fh.close()
                        return
                    kprint("5\n")
                    (buffer:Pointer<Int>)[-1] = length
                    print buffer
            
            fun processCommand(cmd: String)
                if cmd = "cls"
                    clearScreen()
                else if cmd = "version"
                    println("Version 0.0.1")
                else if cmd = "save"
                    writeBlock()
                else if cmd = "load"
                    readBlock()
                else if cmd = "ls"
                    cmdLs()
                else if cmd = "cat"
                    cmdCat()
                else if cmd = "dumpMemory"
                    dumpMemory()
                else
                    println("Unknown command")
            
            """.trimIndent()


        runTest(prog)
    }
}
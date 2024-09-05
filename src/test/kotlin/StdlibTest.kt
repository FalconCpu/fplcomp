import frontend.Lexer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.FileReader
import java.io.FileWriter
import java.io.StringReader

class StdlibTest {

    val stdLibFiles = listOf("hwregs.fpl", "Fatal.fpl", "Memory.fpl", "Print.fpl", "Keyboard.fpl", "Graphics.fpl",
        "StringBuffer.fpl", "LineEditor.fpl")

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

        val emulatorOut = "f32sim.exe -d rom.hex".runCommand()

        val output = emulatorOut?.replace("\r\n", "\n")
        assertEquals(expected, output)
    }


    @Test
    fun helloWorld() {
        val prog = """
            fun main()
                println "Hello, world!"
                printhexln 0," ",100," ",-100
        """.trimIndent()


        val expected = """
            Hello, world!
            00000000 00000064 FFFFFF9C
            
        """.trimIndent()


        runTest(prog, expected)
    }

    @Test
    fun classAlloc() {
        val prog = """
            class Point(val x: Int, val y: Int)
                fun display()
                    printhexln "Point x=", x, " y=", y
                    
            fun main()
                val p = Point(10, 20)
                p.display()
                dumpMemory()
        """.trimIndent()


        val expected = """
            Point x=0000000A y=00000014
            ADDRESS  SIZE     STATUS
            00001000 00000010 Point
            00001010 03F7EFF0 free

        """.trimIndent()


        runTest(prog, expected)
    }

    @Test
    fun arrayAlloc() {
        val prog = """
            fun printArray(a:Array<Int>)
                for i in 0 to <a.size
                    printhexln a[i]
                    
                    
            fun main()
                val a = mutableArrayOf(5,6,4,1)
                printArray(a)
                dumpMemory()
        """.trimIndent()


        val expected = """
            00000005
            00000006
            00000004
            00000001
            ADDRESS  SIZE     STATUS
            00001000 00000018 Array[00000004]
            00001018 03F7EFE8 free
            
        """.trimIndent()


        runTest(prog, expected)
    }

    @Test
    fun localArrayAlloc() {
        val prog = """
            fun printArray(a:Array<Int>)
                for i in 0 to <a.size
                    printhexln a[i]                    
                    
            fun main()
                val a = local arrayOf(5,6,4,1)
                printArray(a)
                dumpMemory()
        """.trimIndent()


        val expected = """
            00000005
            00000006
            00000004
            00000001
            ADDRESS  SIZE     STATUS
            00001000 03F7F000 free

        """.trimIndent()


        runTest(prog, expected)
    }


    @Test
    fun printInt() {
        val prog = """
            fun printArray(a:Array<Int>)
                for i in 0 to <a.size
                    print a[i]," "
                print "\n"
                    
            fun main()
                val a = local arrayOf(5,160216532,-43,1,0)
                printArray(a)
        """.trimIndent()


        val expected = """
            5 160216532 -43 1 0 

        """.trimIndent()


        runTest(prog, expected)
    }

    @Test
    fun keyboardTest() {
        val prog = """
            # The emulator has a built-in set of keycodes it sends to the part.
            # But when running this on the FPGA you can type what you like.
            
            
            
            fun main()
                while  true
                    val key = readKeyboard()
                    if key = KEY_UP
                        print "UP"
                    else if key = KEY_DOWN
                        print "DOWN"
                    else if key = KEY_LEFT
                        print "LEFT"
                    else if key = KEY_RIGHT
                        print "RIGHT"
                    else if key = KEY_PAGE_UP
                        print "PAGEUP"
                    else if key = KEY_PAGE_DOWN
                        print "PAGEDOWN"
                    else if key = KEY_HOME
                        print "HOME"
                    else if key = KEY_END
                        print "END"
                    else if key = KEY_INSERT
                        print "INSERT"
                    else if key = KEY_DELETE
                        print "DELETE"
                    else if key != 0
                        print key
                        
                    if key = 'k'
                        return 
            """.trimIndent()


        val expected = """
            zL(9k
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun graphicsTest() {
        val prog = """
            fun main()
                clearScreen()
                for x in 0 to 15
                    for y in 0 to 15
                        drawRectangle( x*40, y*30, x*40+38, y*30+28, x + 16*y)
                drawString(10, 10, "Hello world", 1)
            """.trimIndent()


        val expected = """
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun printScreenTest() {
        // Currently only works on the FPGA
        val prog = """
            fun main()
                outputChannel = PrintChannel.SCREEN
                clearScreen()
                print "Hello world\n"
                for i in 0 to 10
                    println i
            """.trimIndent()


        val expected = """
        """.trimIndent()

        runTest(prog, expected)

    }

    @Test
    fun osTest() {
        // FPGA only
        val prog = """
            fun main()
                outputChannel = PrintChannel.SCREEN
                clearScreen()
                setFgColor(10)
                println "Falcon Computer System"
                setFgColor(8)
                println systemVars.freeMemory, " bytes free\n"
                setFgColor(11)
                print "> "
                setFgColor(15)
                while true
                    val key = readKeyboard()
                    if key != 0
                        print key
            """.trimIndent()


        val expected = """
            Timeout
            
        """.trimIndent()

        runTest(prog, expected)

    }

    @Test
    fun stringBufferTest() {
        val prog = """
            fun main()
                val sb = StringBuffer()
                sb.append("Hello ")
                sb.append("world")
                sb.insert('!', 5)
                sb.delete(2)
                prnt(sb)
                println ""
                dumpMemory()
            """.trimIndent()


        val expected = """
            Helo! world
            ADDRESS  SIZE     STATUS
            00001000 00000010 StringBuffer
            00001010 00000010 free
            00001020 00000018 Array[00000010]
            00001038 03F7EFC8 free
            
        """.trimIndent()

        runTest(prog, expected)

    }

    @Test
    fun lineEditorTest() {
        val prog = """
            fun main()
                outputChannel = PrintChannel.SCREEN
                clearScreen()
                val le = LineEditor()
                le.run()
            """.trimIndent()


        val expected = """
            Timeout
            
        """.trimIndent()

        runTest(prog, expected)

    }






}
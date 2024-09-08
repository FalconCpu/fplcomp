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
            org 0FFFF0000H
            premain:
            ld %sp, 4000000H
            jsr initializeMemory()
            jsr main()
            ld %1, 0
            ld %2, 0
            jsr fatal(Int,Int)

            main():
            sub %sp, %sp, 4
            stw %30, %sp[0]
            ld %1, "Hello, world!"
            jsr print(String)
            ld %1, 42
            jsr print(Int)
            jsr printNewline()
            ldw %30, %sp[0]
            add %sp, %sp, 4
            ret

            String|class:
            dcw "String"
            dcw 0

        """.trimIndent()


        runTest(prog, expected)
    }


    @Test
    fun arrayTest() {
        val prog = """
            fun sum(array:Array<Int>)->Int
                var sum = 0
                var index = 0
                while index < array.size
                    sum = sum + array[index]
                    index = index + 1
                return sum
                
            fun main()
                val arr = arrayOf(1, 2, 3, 4, 5)
                val arr2 = mutableArrayOf(1, 2, 3, 4, 5)
                println sum(arr)
        """.trimIndent()

        val expected = """
            org 0FFFF0000H
            premain:
            ld %sp, 4000000H
            jsr initializeMemory()
            jsr main()
            ld %1, 0
            ld %2, 0
            jsr fatal(Int,Int)

            sum(Array<Int>):
            ld %8, 0
            ld %2, 0
            jmp @2
            @1:
            lsl %3, %2, 2
            add %3, %1, %3
            ldw %3, %3[0]
            add %8, %8, %3
            add %2, %2, 1
            @2:
            ldw %3, %1[-4]
            blt %2, %3, @1
            ret

            main():
            sub %sp, %sp, 8
            stw %9, %sp[0]
            stw %30, %sp[4]
            ld %9, Array|0
            ld %1, 20
            ld %2, MutableArray<Int>|class
            jsr malloc(Int,Pointer)
            ld %1, 5
            stw %1, %8[-4]
            ld %1, 1
            stw %1, %8[0]
            ld %1, 2
            stw %1, %8[4]
            ld %1, 3
            stw %1, %8[8]
            ld %1, 4
            stw %1, %8[12]
            ld %1, 5
            stw %1, %8[16]
            ld %1, %9
            jsr sum(Array<Int>)
            ld %1, %8
            jsr print(Int)
            jsr printNewline()
            ldw %9, %sp[0]
            ldw %30, %sp[4]
            add %sp, %sp, 8
            ret

            Array<Int>|class:
            dcw "Array<Int>"
            dcw 0

            MutableArray<Int>|class:
            dcw "MutableArray<Int>"
            dcw 0

            String|class:
            dcw "String"
            dcw 0
            dcw 5
            Array|0:
            dcw 1
            dcw 2
            dcw 3
            dcw 4
            dcw 5


        """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun nullCheckTest() {
        val prog = """
            fun foo(a:Pointer<Int>?)->Int
                val b = a!!
                return a[2]
        """.trimIndent()

        val expected = """
            org 0FFFF0000H
            premain:
            ld %sp, 4000000H
            jsr initializeMemory()
            ld %1, 0
            ld %2, 0
            jsr fatal(Int,Int)

            foo(Pointer<Int>?):
            sub %sp, %sp, 4
            stw %30, %sp[0]
            beq %1, 0, @1
            ld %2, 2
            lsl %2, %2, 2
            add %1, %1, %2
            ldw %8, %1[0]
            ldw %30, %sp[0]
            add %sp, %sp, 4
            ret

            @1:
            ld %1, 4
            ld %1, 0
            jsr fatal(Int,Int)
            ldw %30, %sp[0]
            add %sp, %sp, 4
            ret

            Pointer<Int>|class:
            dcw "Pointer<Int>"
            dcw 0

            String|class:
            dcw "String"
            dcw 0

        """.trimIndent()

        runTest(prog, expected)
    }

}
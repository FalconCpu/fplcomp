import frontend.Lexer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class IrgenTest {
    private fun runTest(prog: String, expected: String) {
        val lexer = Lexer("test.txt", StringReader(prog))
        val output = compile(listOf(lexer), StopAt.IRGEN)
        assertEquals(expected, output)
    }

    @Test
    fun simpleDeclaration() {
        val prog = """
            fun main()
                val x = 10           
        """.trimIndent()

        val expected = """
            Function <top>
            start
            call main()
            end

            Function main()
            start
            mov t0, 10
            mov x, t0
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun simpleArithmetic() {
        val prog = """
            fun main()
                val x = 10
                val y = x + 5
        """.trimIndent()

        val expected = """
            Function <top>
            start
            call main()
            end

            Function main()
            start
            mov t0, 10
            mov x, t0
            mov t1, 5
            add t2, x, t1
            mov y, t2
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun whileLoop() {
        val prog = """
            fun main()
                var x = 0
                while x < 10
                    x = x + 1
        """.trimIndent()

        val expected = """
            Function <top>
            start
            call main()
            end

            Function main()
            start
            mov t0, 0
            mov x, t0
            jmp @2
            @1:
            mov t1, 1
            add t2, x, t1
            mov x, t2
            @2:
            mov t3, 10
            blt, x, t3, @1
            jmp @3
            @3:
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun helloWorld() {
        val prog = """
            fun main()
                println "Hello, world!", 42
        """.trimIndent()

        val expected = """
            Function <top>
            start
            call main()
            end

            Function main()
            start
            lea t0, "Hello, world!"
            mov %1, t0
            call print(String)
            mov t1, 42
            mov %1, t1
            call print(Int)
            call printNewline()
            @0:
            end


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
            Function <top>
            start
            call main()
            end

            Function sum(Array<Int>)
            start
            mov array, %1
            mov t0, 0
            mov sum, t0
            mov t1, 0
            mov index, t1
            jmp @2
            @1:
            lsl t2, index, 2
            add t3, array, t2
            ldw t4, t3[0]
            add t5, sum, t4
            mov sum, t5
            mov t6, 1
            add t7, index, t6
            mov index, t7
            @2:
            ldw t8, array->size
            blt, index, t8, @1
            jmp @3
            @3:
            mov %8, sum
            jmp @0
            @0:
            end

            Function main()
            start
            lea t0, Array|0
            mov arr, t0
            mov %1, 5
            mov %2, 4
            call mallocArray(Int,Int)
            mov t1, %8
            mov t2, 1
            stw t2, t1[0]
            mov t3, 2
            stw t3, t1[4]
            mov t4, 3
            stw t4, t1[8]
            mov t5, 4
            stw t5, t1[12]
            mov t6, 5
            stw t6, t1[16]
            mov arr2, t1
            mov %1, arr
            call sum(Array<Int>)
            mov t7, %8
            mov %1, t7
            call print(Int)
            call printNewline()
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun ifTest() {
        val prog = """
            fun compare_to_10(x:Int)
                if x < 5
                    println x," is less than 5"
                else
                    println x," is greater than or equal to 5"
                    
            fun main()
                var count = 0
                while count < 20
                    compare_to_10(count)
                    count = count + 1
        """.trimIndent()

        val expected = """
            Function <top>
            start
            call main()
            end

            Function compare_to_10(Int)
            start
            mov x, %1
            mov t0, 5
            blt, x, t0, @2
            jmp @3
            @3:
            jmp @4
            jmp @1
            @2:
            mov %1, x
            call print(Int)
            lea t1, " is less than 5"
            mov %1, t1
            call print(String)
            call printNewline()
            jmp @1
            @4:
            mov %1, x
            call print(Int)
            lea t2, " is greater than or equal to 5"
            mov %1, t2
            call print(String)
            call printNewline()
            jmp @1
            @1:
            @0:
            end

            Function main()
            start
            mov t0, 0
            mov count, t0
            jmp @2
            @1:
            mov %1, count
            call compare_to_10(Int)
            mov t1, %8
            mov t2, 1
            add t3, count, t2
            mov count, t3
            @2:
            mov t4, 20
            blt, count, t4, @1
            jmp @3
            @3:
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun forTest() {
        val prog = """
            fun main()
                for i in 1 to 10
                    println i
        """.trimIndent()

        val expected = """
            Function <top>
            start
            call main()
            end

            Function main()
            start
            mov t0, 1
            mov t1, 10
            mov i, t0
            jmp @2
            @1:
            mov %1, i
            call print(Int)
            call printNewline()
            add t2, i, 1
            mov i, t2
            @2:
            ble, i, t1, @1
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun methodCalls() {
        val prog = """
            class Cat(val name:String, val age:Int) 
                fun greet()
                    println name," says hello"
            
            fun main()
                val cat = Cat("Whiskers", 4)
                cat.greet()
        """.trimIndent()

        val expected = """
            Function <top>
            start
            call main()
            end

            Function Cat/greet()
            start
            mov this, %1
            ldw t0, this->name
            mov %1, t0
            call print(String)
            lea t1, " says hello"
            mov %1, t1
            call print(String)
            call printNewline()
            @0:
            end

            Function main()
            start
            lea %1, Cat
            call mallocObject(ClassDescriptor)
            mov t0, %8
            lea t1, "Whiskers"
            mov t2, 4
            mov %1, t0
            mov %2, t1
            mov %3, t2
            call Cat
            mov t3, %8
            mov cat, t0
            mov %1, cat
            call Cat/greet()
            mov t4, %8
            @0:
            end

            Function Cat
            start
            mov this, %1
            stw %2, this->name
            stw %3, this->age
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun methodCalls3() {
        val prog = """
           class Animal(val name:String)
                open fun greet()
                    println name, " says grunt"
            
           class Cat(name: String) : Animal(name)
               override fun greet()
                    println name, " says meow"

           fun main()
               val cat = Cat("Whiskers")
               cat.greet()
       """.trimIndent()

        val expected = """
            Function <top>
            start
            call main()
            end

            Function Animal/greet()
            start
            mov this, %1
            ldw t0, this->name
            mov %1, t0
            call print(String)
            lea t1, " says grunt"
            mov %1, t1
            call print(String)
            call printNewline()
            @0:
            end

            Function Cat/greet()
            start
            mov this, %1
            ldw t0, this->name
            mov %1, t0
            call print(String)
            lea t1, " says meow"
            mov %1, t1
            call print(String)
            call printNewline()
            @0:
            end

            Function main()
            start
            lea %1, Cat
            call mallocObject(ClassDescriptor)
            mov t0, %8
            lea t1, "Whiskers"
            mov %1, t0
            mov %2, t1
            call Cat
            mov t2, %8
            mov cat, t0
            mov %1, cat
            virtcall cat, Cat/greet()
            mov t3, %8
            @0:
            end

            Function Animal
            start
            mov this, %1
            stw %2, this->name
            @0:
            end

            Function Cat
            start
            mov this, %1
            mov name, %2
            mov %1, this
            mov %2, name
            call Animal
            mov t0, %8
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun boolAndTest() {
        val prog = """
           fun main(a:Int, b:Int) -> Bool
               val bool = (a = 1) and (b = 2)
               return bool
       """.trimIndent()

        val expected = """
            Function <top>
            start
            call main(Int,Int)
            end

            Function main(Int,Int)
            start
            mov a, %1
            mov b, %2
            mov t1, 1
            ceq t2, a, t1
            mov t0, t2
            beq, t2, 0, @1
            mov t3, 2
            ceq t4, b, t3
            mov t0, t4
            @1:
            mov bool, t0
            mov %8, bool
            jmp @0
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun boolOrTest() {
        val prog = """
           fun main(a:Int, b:Int) -> Bool
               val bool = (a = 1) or (b = 2)
               return bool
       """.trimIndent()

        val expected = """
            Function <top>
            start
            call main(Int,Int)
            end

            Function main(Int,Int)
            start
            mov a, %1
            mov b, %2
            mov t1, 1
            ceq t2, a, t1
            mov t0, t2
            bne, t2, 0, @1
            mov t3, 2
            ceq t4, b, t3
            mov t0, t4
            @1:
            mov bool, t0
            mov %8, bool
            jmp @0
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun negTest() {
        val prog = """
           fun main(a:Int) -> Int
               return -a
       """.trimIndent()

        val expected = """
            Function <top>
            start
            call main(Int)
            end

            Function main(Int)
            start
            mov a, %1
            sub t0, 0, a
            mov %8, t0
            jmp @0
            @0:
            end

            
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun arrayConstructor() {
        val prog = """
           fun main(size:Int) -> Array<Int>
               return Array<Int>(size)
       """.trimIndent()

        val expected = """
            Function <top>
            start
            call main(Int)
            end

            Function main(Int)
            start
            mov size, %1
            mov %1, size
            mov %2, 4
            call mallocArray(Int,Int)
            mov t0, %8
            mov %8, t0
            jmp @0
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun compareRvalue() {
        val prog = """
           fun main(a:Int, b:Int) -> Bool
               return a<=b
       """.trimIndent()

        val expected = """
            Function <top>
            start
            call main(Int,Int)
            end

            Function main(Int,Int)
            start
            mov a, %1
            mov b, %2
            cle t0, a, b
            mov %8, t0
            jmp @0
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun globalVar() {
        val prog = """
           var count = 0
            
           fun main() -> Int
               count = count + 1
               return count
       """.trimIndent()

        val expected = """
            Function <top>
            start
            mov t0, 0
            stw t0, GLOBAL->count
            call main()
            end

            Function main()
            start
            ldw t0, GLOBAL->count
            mov t1, 1
            add t2, t0, t1
            stw t2, GLOBAL->count
            ldw t3, GLOBAL->count
            mov %8, t3
            jmp @0
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun fibonacciTest() {
        val prog = """
            fun fib(n:Int) -> Int   
                if n <= 2
                    return n
                return fib(n - 1) + fib(n - 2)

            fun main()
                for i in 1 to 10
                    println fib(i)
            """.trimIndent()

        val expected = """
            Function <top>
            start
            call main()
            end

            Function fib(Int)
            start
            mov n, %1
            mov t0, 2
            ble, n, t0, @2
            jmp @3
            @3:
            jmp @1
            @2:
            mov %8, n
            jmp @0
            jmp @1
            @1:
            mov t1, 1
            sub t2, n, t1
            mov %1, t2
            call fib(Int)
            mov t3, %8
            mov t4, 2
            sub t5, n, t4
            mov %1, t5
            call fib(Int)
            mov t6, %8
            add t7, t3, t6
            mov %8, t7
            jmp @0
            @0:
            end

            Function main()
            start
            mov t0, 1
            mov t1, 10
            mov i, t0
            jmp @2
            @1:
            mov %1, i
            call fib(Int)
            mov t2, %8
            mov %1, t2
            call print(Int)
            call printNewline()
            add t3, i, 1
            mov i, t3
            @2:
            ble, i, t1, @1
            @0:
            end


            """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun constTest() {
        val prog = """
            const TWO = 2
            
            fun main()
                print TWO
        """.trimIndent()

        val expected = """
            Function <top>
            start
            call main()
            end

            Function main()
            start
            mov t0, 2
            mov %1, t0
            call print(Int)
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun constArray() {
        val prog = """    
            val myArray = arrayOf(5,8,10,12)
            
            fun printArray(a:Array<Int>)
                for i in 0 to <a.size
                    print a[i]," "
                print "\n"
                    
            fun main()
                printArray(myArray)
        """.trimIndent()


        val expected = """
            Function <top>
            start
            lea t0, Array|0
            stw t0, GLOBAL->myArray
            call main()
            end

            Function printArray(Array<Int>)
            start
            mov a, %1
            mov t0, 0
            ldw t1, a->size
            mov i, t0
            jmp @2
            @1:
            lsl t2, i, 2
            add t3, a, t2
            ldw t4, t3[0]
            mov %1, t4
            call print(Int)
            lea t5, " "
            mov %1, t5
            call print(String)
            add t6, i, 1
            mov i, t6
            @2:
            blt, i, t1, @1
            lea t7, "\n"
            mov %1, t7
            call print(String)
            @0:
            end

            Function main()
            start
            ldw t0, GLOBAL->myArray
            mov %1, t0
            call printArray(Array<Int>)
            mov t1, %8
            @0:
            end            


        """.trimIndent()


        runTest(prog, expected)
    }



}
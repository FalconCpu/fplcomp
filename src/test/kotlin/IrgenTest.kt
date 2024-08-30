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
            call main
            end

            Function main
            start
            t0 = 10
            x = t0
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
            call main
            end

            Function main
            start
            t0 = 10
            x = t0
            t1 = 5
            t2 = x + t1
            y = t2
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
            call main
            end

            Function main
            start
            t0 = 0
            x = t0
            jmp @2
            @1:
            t1 = 1
            t2 = x + t1
            x = t2
            @2:
            t3 = 10
            if x < t3 jmp @1
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
            call main
            end

            Function main
            start
            t0 = ADDR("Hello, world!")
            %1 = t0
            call StdlibPrintString
            t1 = 42
            %1 = t1
            call StdlibPrintInt
            call StdlibNewline
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
                println sum(arr)
        """.trimIndent()

        val expected = """
            Function <top>
            start
            call main
            end

            Function sum
            start
            array = %1
            t0 = 0
            sum = t0
            t1 = 0
            index = t1
            jmp @2
            @1:
            t2 = array[index]
            t3 = sum + t2
            sum = t3
            t4 = 1
            t5 = index + t4
            index = t5
            @2:
            t6 = array->size
            if index < t6 jmp @1
            jmp @3
            @3:
            %8 = sum
            jmp @0
            @0:
            end

            Function main
            start
            %1 = 5
            %2 = 4
            call StdlibMallocArray
            t0 = %8
            t1 = 1
            t0[0] = t1
            t2 = 2
            t0[1] = t2
            t3 = 3
            t0[2] = t3
            t4 = 4
            t0[3] = t4
            t5 = 5
            t0[4] = t5
            arr = t0
            %1 = arr
            call sum
            t6 = %8
            %1 = t6
            call StdlibPrintInt
            call StdlibNewline
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
            call main
            end

            Function compare_to_10
            start
            x = %1
            t0 = 5
            if x < t0 jmp @2
            jmp @3
            @3:
            jmp @4
            @1:
            @2:
            %1 = x
            call StdlibPrintInt
            t1 = ADDR(" is less than 5")
            %1 = t1
            call StdlibPrintString
            call StdlibNewline
            jmp @1
            @4:
            %1 = x
            call StdlibPrintInt
            t2 = ADDR(" is greater than or equal to 5")
            %1 = t2
            call StdlibPrintString
            call StdlibNewline
            jmp @1
            @1:
            @0:
            end

            Function main
            start
            t0 = 0
            count = t0
            jmp @2
            @1:
            %1 = count
            call compare_to_10
            t1 = %8
            t2 = 1
            t3 = count + t2
            count = t3
            @2:
            t4 = 20
            if count < t4 jmp @1
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
            call main
            end

            Function main
            start
            t0 = 1
            t1 = 10
            i = t0
            jmp @2
            @1:
            %1 = i
            call StdlibPrintInt
            call StdlibNewline
            t2 = i + 1
            i = t2
            @2:
            if i <= t1 jmp @1
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
            call main
            end

            Function Cat/greet
            start
            this = %1
            t0 = this->name
            %1 = t0
            call StdlibPrintString
            t1 = ADDR(" says hello")
            %1 = t1
            call StdlibPrintString
            call StdlibNewline
            @0:
            end

            Function main
            start
            %1 = ADDR(Cat)
            call StdlibMallocObject
            t0 = %8
            t1 = ADDR("Whiskers")
            t2 = 4
            %1 = t0
            %2 = t1
            %3 = t2
            call Cat
            t3 = %8
            cat = t0
            %1 = cat
            call Cat/greet
            t4 = %8
            @0:
            end

            Function Cat
            start
            this = %1
            this->name = %2
            this->age = %3
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun methodCalls3(){
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
            call main
            end

            Function Animal/greet
            start
            this = %1
            t0 = this->name
            %1 = t0
            call StdlibPrintString
            t1 = ADDR(" says grunt")
            %1 = t1
            call StdlibPrintString
            call StdlibNewline
            @0:
            end

            Function Cat/greet
            start
            this = %1
            t0 = this->name
            %1 = t0
            call StdlibPrintString
            t1 = ADDR(" says meow")
            %1 = t1
            call StdlibPrintString
            call StdlibNewline
            @0:
            end

            Function main
            start
            %1 = ADDR(Cat)
            call StdlibMallocObject
            t0 = %8
            t1 = ADDR("Whiskers")
            %1 = t0
            %2 = t1
            call Cat
            t2 = %8
            cat = t0
            %1 = cat
            virtcall cat, greet
            t3 = %8
            @0:
            end

            Function Animal
            start
            this = %1
            this->name = %2
            @0:
            end

            Function Cat
            start
            this = %1
            name = %2
            %1 = this
            %2 = name
            call Animal
            t0 = %8
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun boolAndTest(){
        val prog = """
           fun main(a:Int, b:Int) -> Bool
               val bool = (a = 1) and (b = 2)
               return bool
       """.trimIndent()

        val expected = """
            Function <top>
            start
            call main
            end

            Function main
            start
            a = %1
            b = %2
            t1 = 1
            t2 = a == t1
            t0 = t2
            if t2 == 0 jmp @1
            t3 = 2
            t4 = b == t3
            t0 = t4
            @1:
            bool = t0
            %8 = bool
            jmp @0
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun boolOrTest(){
        val prog = """
           fun main(a:Int, b:Int) -> Bool
               val bool = (a = 1) or (b = 2)
               return bool
       """.trimIndent()

        val expected = """
            Function <top>
            start
            call main
            end

            Function main
            start
            a = %1
            b = %2
            t1 = 1
            t2 = a == t1
            t0 = t2
            if t2 != 0 jmp @1
            t3 = 2
            t4 = b == t3
            t0 = t4
            @1:
            bool = t0
            %8 = bool
            jmp @0
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun negTest(){
        val prog = """
           fun main(a:Int) -> Int
               return -a
       """.trimIndent()

        val expected = """
            Function <top>
            start
            call main
            end

            Function main
            start
            a = %1
            t0 = 0 - a
            %8 = t0
            jmp @0
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun arrayConstructor(){
        val prog = """
           fun main(size:Int) -> Array<Int>
               return Array<Int>(size)
       """.trimIndent()

        val expected = """
            Function <top>
            start
            call main
            end

            Function main
            start
            size = %1
            %1 = size
            call StdlibMallocArray
            t0 = %8
            %8 = t0
            jmp @0
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun compareRvalue(){
        val prog = """
           fun main(a:Int, b:Int) -> Bool
               return a<=b
       """.trimIndent()

        val expected = """
            Function <top>
            start
            call main
            end

            Function main
            start
            a = %1
            b = %2
            t0 = a <= b
            %8 = t0
            jmp @0
            @0:
            end


        """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun globalVar(){
        val prog = """
           var count = 0
            
           fun main() -> Int
               count = count + 1
               return count
       """.trimIndent()

        val expected = """
        """.trimIndent()

        runTest(prog, expected)
    }


}
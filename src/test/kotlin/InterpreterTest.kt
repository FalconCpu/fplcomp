import backend.runInterpreter
import frontend.Lexer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

private val debug = false

class InterpreterTest {

    private fun runTest(prog: String, expected: String) {
        val lexer = Lexer("test.txt", StringReader(prog))
        val prog = compile(listOf(lexer), StopAt.IRGEN)

        if (Log.anyError())
            error(Log.dump())

        if (debug)
            println(prog)

        val output = runInterpreter()
        assertEquals(expected, output)
    }

    @Test
    fun helloWorld() {
        val prog = """
            fun main()
                println "Hello, world!", 42
        """.trimIndent()

        val expected = """
            Hello, world!42
            
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun arithOperation() {
        val prog = """
            fun main()
                val a = 10
                val b = 5
            
                # Addition
                val add = a + b
                println "Addition result: ", add
            
                # Subtraction
                val sub = a - b
                println "Subtraction result: ", sub
            
                # Multiplication
                val mul = a * b
                println "Multiplication result: ", mul
            
                # Division
                val div = a / b
                println "Division result: ", div
            
                # Modulus
                val mod = a % b
                println "Modulus result: ", mod
            
                # Complex expression
                val complex = (a + b) * (a - b) / (b + 1)
                println "Complex expression result: ", complex
        """.trimIndent()

        val expected = """
            Addition result: 15
            Subtraction result: 5
            Multiplication result: 50
            Division result: 2
            Modulus result: 0
            Complex expression result: 12

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun whileLoop() {
        val prog = """
            fun main()
                var i = 0
                while i < 5
                    println "Current value of i: ", i
                    i = i + 1
                println "Loop finished."
        """.trimIndent()

        val expected = """
            Current value of i: 0
            Current value of i: 1
            Current value of i: 2
            Current value of i: 3
            Current value of i: 4
            Loop finished.

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
            15

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun ifTest() {
        val prog = """
            fun compare_to_5(x:Int)
                if x < 5
                    println x," is less than 5"
                else if x = 5
                    println x," is equal to 5"
                else
                    println x," is greater than to 5"
                    
            fun main()
                var count = 0
                while count < 20
                    compare_to_5(count)
                    count = count + 1
        """.trimIndent()

        val expected = """
            0 is less than 5
            1 is less than 5
            2 is less than 5
            3 is less than 5
            4 is less than 5
            5 is equal to 5
            6 is greater than to 5
            7 is greater than to 5
            8 is greater than to 5
            9 is greater than to 5
            10 is greater than to 5
            11 is greater than to 5
            12 is greater than to 5
            13 is greater than to 5
            14 is greater than to 5
            15 is greater than to 5
            16 is greater than to 5
            17 is greater than to 5
            18 is greater than to 5
            19 is greater than to 5

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun forTest() {
        val prog = """
            fun main()
                for i in 1 to 10
                    println i
                for i in 4 to <6
                    println i
        """.trimIndent()

        val expected = """
            1
            2
            3
            4
            5
            6
            7
            8
            9
            10
            4
            5

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
            Whiskers says hello

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun methodCalls2(){
       val prog = """
           class Cat(val name: String, val age: Int)
               fun greet(greeting: String) -> String
                   return name + " says " + greeting

           fun main()
               val cat = Cat("Whiskers", 4)
               println cat.greet("meow")
       """.trimIndent()

        val expected = """
            Whiskers says meow

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
            Whiskers says meow

        """.trimIndent()

        runTest(prog, expected)

    }


    @Test
    fun methodCalls4(){
        val prog = """
           class Animal(val name:String)
                open fun greet()
                    println name, " says grunt"
            
           class Cat(name: String) : Animal(name)
               override fun greet()
                    println name, " says meow"

           fun main()
               val c = Cat("Whiskers")
               c.greet()
               
               val a : Animal = c
               a.greet()
       """.trimIndent()

        val expected = """
            Whiskers says meow
            Whiskers says meow

        """.trimIndent()

        runTest(prog, expected)

    }

    @Test
    fun fibonacciTest() {
        val prog = """
            fun fib(n:Int) -> Int   
                if n < 2
                    return n
                return fib(n - 1) + fib(n - 2)

            fun main()
                for i in 0 to 10
                    println fib(i)
            """.trimIndent()

        val expected = """
            0
            1
            1
            2
            3
            5
            8
            13
            21
            34
            55

            """.trimIndent()

        runTest(prog, expected)
    }


}
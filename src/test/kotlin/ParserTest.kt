package falcon
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class ParserTest {

    private fun runTest(prog: String, expected: String) {
        val lexer = Lexer("test.txt", StringReader(prog))
        val output = compile(listOf(lexer), StopAt.AST)
        assertEquals(expected, output)
    }

    @Test
    fun simpleExpression() {
        val prog = """
            val a = 10
            val b = (a + 1)*2
        """.trimIndent()

        val expected = """
            TOP
            . val a
            . . INTLIT 10
            . val b
            . . BINARYOP *
            . . . BINARYOP +
            . . . . IDENTIFIER a
            . . . . INTLIT 1
            . . . INTLIT 2

            """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun missingEol() {
        val prog = """
            val a = 10
            val b = (a + 1)*2 45
        """.trimIndent()

        val expected = """
            test.txt 2.19:- Got '45' when expecting end of line
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun badStatement() {
        val prog = """
            then a = 10
        """.trimIndent()

        val expected = """
            test.txt 1.1:- Got 'then' when expecting statement
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun declarationWithType() {
        val prog = """
            val a : Int = 10
        """.trimIndent()

        val expected = """
            TOP
            . val a
            . . TYPEID Int
            . . INTLIT 10

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun malformedIntLit() {
        val prog = """
            val a = 10X
        """.trimIndent()

        val expected = """
            test.txt 1.9:- Invalid integer literal: 10X
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun badPrimaryExpression() {
        val prog = """
            val a = )10
        """.trimIndent()

        val expected = """
            test.txt 1.9:- Got ')' when expecting primary expression
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun memberAccess() {
        val prog = """
            val a = 10
            val b = a.x
        """.trimIndent()

        val expected = """
            TOP
            . val a
            . . INTLIT 10
            . val b
            . . MEMBERACCESS x
            . . . IDENTIFIER a

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun parseFuncCall() {
        val prog = """
            val x = double(10)
            val y = fred(a,"hello")
        """.trimIndent()

        val expected = """
            TOP
            . val x
            . . FUNCCALL
            . . . IDENTIFIER double
            . . . INTLIT 10
            . val y
            . . FUNCCALL
            . . . IDENTIFIER fred
            . . . IDENTIFIER a
            . . . STRINGLIT hello

        """.trimIndent()
        runTest(prog, expected)
    }

    @Test
    fun parseArrayAccess() {
        val prog = """
            val b = a[1]
        """.trimIndent()

        val expected = """
            TOP
            . val b
            . . ARRAYACCESS
            . . . IDENTIFIER a
            . . . INTLIT 1

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun parseFunction() {
        val prog = """
            fun double(a:Int)->Int
                return a*2
                
            fun main()
                val x = double(10)
        """.trimIndent()


        val expected = """
            TOP
            . FUNCTION double
            . . PARAMETER a
            . . . TYPEID Int
            . . TYPEID Int
            . . RETURN
            . . . BINARYOP *
            . . . . IDENTIFIER a
            . . . . INTLIT 2
            . FUNCTION main
            . . val x
            . . . FUNCCALL
            . . . . IDENTIFIER double
            . . . . INTLIT 10

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun badEnd() {
        val prog = """
            fun main()
                val x = 10
            end if
        """.trimIndent()

        val expected = """  
            test.txt 3.5:- Got 'if' when expecting 'fun'
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun arrayParam() {
        val prog = """
            fun sum(a:Array<Int>)->Int
                var total = 0
        """.trimIndent()

        val expected = """
              TOP
              . FUNCTION sum
              . . PARAMETER a
              . . . TYPEARRAY
              . . . . TYPEID Int
              . . TYPEID Int
              . . var total
              . . . INTLIT 0

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun functionTypes() {
        val prog = """
            fun doSomething( a:(Int,Int)->String ) -> Array<Int>?
                return null
        """.trimIndent()

        val expected = """  
            TOP
            . FUNCTION doSomething
            . . PARAMETER a
            . . . TYPEFUNCTION
            . . . . TYPEID Int
            . . . . TYPEID Int
            . . . . TYPEID String
            . . TYPENULLABLE
            . . . TYPEARRAY
            . . . . TYPEID Int
            . . RETURN
            . . . IDENTIFIER null

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun whileTest() {
        val prog = """
            fun main()
                var x = 0
                while x < 10
                    x = x + 1
                end while
        """.trimIndent()

        val expected = """  
            TOP
            . FUNCTION main
            . . var x
            . . . INTLIT 0
            . . WHILE
            . . . BINARYOP <
            . . . . IDENTIFIER x
            . . . . INTLIT 10
            . . . ASSIGN
            . . . . IDENTIFIER x
            . . . . BINARYOP +
            . . . . . IDENTIFIER x
            . . . . . INTLIT 1

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun exprStatement() {
        val prog = """
            fun main()
                doSomething()
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main
            . . EXPR
            . . . FUNCCALL
            . . . . IDENTIFIER doSomething

        """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun andOr() {
        val prog = """
            fun main()
                while a<b and c>d or e!=f
                    doSomething()
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main
            . . WHILE
            . . . BINARYOP or
            . . . . BINARYOP and
            . . . . . BINARYOP <
            . . . . . . IDENTIFIER a
            . . . . . . IDENTIFIER b
            . . . . . BINARYOP >
            . . . . . . IDENTIFIER c
            . . . . . . IDENTIFIER d
            . . . . NEQ
            . . . . . IDENTIFIER e
            . . . . . IDENTIFIER f
            . . . EXPR
            . . . . FUNCCALL
            . . . . . IDENTIFIER doSomething

            """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun typeInBrackets() {
        val prog = """
            fun main(a:(Int))->Int
                return 1
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main
            . . PARAMETER a
            . . . TYPEID Int
            . . TYPEID Int
            . . RETURN
            . . . INTLIT 1

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun badMultipleTypes() {
        val prog = """
            fun main(a:(Int,Int))->Int
                return 1
        """.trimIndent()

        val expected = """ 
             test.txt 1.12:- Cannot have multiple types
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun badTypeExpression() {
        val prog = """
            fun main(a:4)->Int
                return 1
        """.trimIndent()

        val expected = """ 
             test.txt 1.12:- Expected type, got <integer literal>
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun missingType() {
        val prog = """
            fun main(a:())->Int
                return 1
        """.trimIndent()

        val expected = """ 
             test.txt 1.12:- Missing type
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun badIndentation() {
        val prog = """
            fun main()->Int
                doSomething()
                    return 1
        """.trimIndent()

        val expected = """ 
            test.txt 3.9:- Unexpected indentation
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun ifTest() {
        val prog = """
            fun main(a:Int)->String
                if a=0 
                    return "zero"
                else if a=1
                    return "one"
                else
                    return "lots"
        """.trimIndent()

        val expected = """  
            TOP
            . FUNCTION main
            . . PARAMETER a
            . . . TYPEID Int
            . . TYPEID String
            . . IF
            . . . CLAUSE
            . . . . EQ
            . . . . . IDENTIFIER a
            . . . . . INTLIT 0
            . . . . RETURN
            . . . . . STRINGLIT zero
            . . . CLAUSE
            . . . . EQ
            . . . . . IDENTIFIER a
            . . . . . INTLIT 1
            . . . . RETURN
            . . . . . STRINGLIT one
            . . . CLAUSE
            . . . . RETURN
            . . . . . STRINGLIT lots

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun badTwoElse() {
        val prog = """
            fun main(a:Int)->String
                if a=0
                    return "zero"
                else
                    return "one"
                else
                    return "lots"
        """.trimIndent()

        val expected = """
            test.txt 4.5:- Else clause must be at the end
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun classNoBody() {
        val prog = """
            class Cat(val name:String, var age:Int)
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Cat
            . . PARAMETER name val
            . . . TYPEID String
            . . PARAMETER age var
            . . . TYPEID Int

        """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun classWithBody() {
        val prog = """
            class Cat(val name:String, var age:Int)
                val legs = 4
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Cat
            . . PARAMETER name val
            . . . TYPEID String
            . . PARAMETER age var
            . . . TYPEID Int
            . . val legs
            . . . INTLIT 4

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun missingIfBody() {
        val prog = """
            fun main(a:Int)->String
                if a=0
                return "zero"
        """.trimIndent()

        val expected = """
            test.txt 3.5:- Missing if body
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun missingFunctionBody() {
        val prog = """
            fun main(a:Int)->String
            
        """.trimIndent()

        val expected = """
            test.txt 2.1:- Missing function body
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun missingWhileBody() {
        val prog = """
            fun main(a:Int)->String
                while a=0
                
        """.trimIndent()

        val expected = """
            test.txt 3.5:- Missing while body
        """.trimIndent()

        runTest(prog, expected)
    }

    fun emptyTest() {
        val prog = """
        """.trimIndent()

        val expected = """  
            """.trimIndent()

        runTest(prog, expected)
    }

}
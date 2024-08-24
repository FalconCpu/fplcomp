package falcon

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class TypeCheck {
    private fun runTest(prog: String, expected: String) {
        val lexer = Lexer("test.txt", StringReader(prog))
        val output = compile(listOf(lexer), StopAt.TYPECHECK)
        assertEquals(expected, output)
    }

    @Test
    fun simpleDeclaration() {
        val prog = """
            val a = 1
            val b = a
            var c = b + 1
        """.trimIndent()

        val expected = """
            TOP
            . DECL GLOBALVAR a Int
            . . INTLIT 1 Int
            . DECL GLOBALVAR b Int
            . . IDENTIFIER GLOBALVAR a Int
            . DECL GLOBALVAR c Int
            . . BINARYOP + Int
            . . . IDENTIFIER GLOBALVAR b Int
            . . . INTLIT 1 Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun duplicateDeclaration() {
        val prog = """
            val a = 1
            val a = 2
        """.trimIndent()

        val expected = """
            test.txt 2.5:- duplicate symbol: a
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun undefinedType() {
        val prog = """
            val a
        """.trimIndent()

        val expected = """
            test.txt 1.5:- Unknown type for a
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun declBadType() {
        val prog = """
            val a : Int = "hello"
        """.trimIndent()

        val expected = """
            test.txt 1.15:- Got type String when expecting Int
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun assignment() {
        val prog = """
            fun count(a:Int)->Int
                var sum = 0
                var count =0
                while count < a
                    sum = sum + count
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION count (Int)->Int
            . . DECL LOCALVAR sum Int
            . . . INTLIT 0 Int
            . . DECL LOCALVAR count Int
            . . . INTLIT 0 Int
            . . WHILE
            . . . BINARYOP < Bool
            . . . . IDENTIFIER LOCALVAR count Int
            . . . . IDENTIFIER LOCALVAR a Int
            . . . ASSIGN
            . . . . IDENTIFIER LOCALVAR sum Int
            . . . . BINARYOP + Int
            . . . . . IDENTIFIER LOCALVAR sum Int
            . . . . . IDENTIFIER LOCALVAR count Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun functionCall() {
        val prog = """
            fun double(a:Int)->Int
                return a*2
                
            fun main()
                val x = double(10)
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION double (Int)->Int
            . . RETURN
            . . . BINARYOP * Int
            . . . . IDENTIFIER LOCALVAR a Int
            . . . . INTLIT 2 Int
            . FUNCTION main ()->Unit
            . . DECL LOCALVAR x Int
            . . . FUNCCALL Int
            . . . . IDENTIFIER FUNC double (Int)->Int
            . . . . INTLIT 10 Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun functionCallWrongNumArgs() {
        val prog = """
            fun double(a:Int)->Int
                return a*2
                
            fun main()
                val x = double(10,5)
        """.trimIndent()

        val expected = """
            test.txt 5.13:- Got 2 arguments when expecting 1
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun functionCallWrongArgType() {
        val prog = """
            fun double(a:Int)->Int
                return a*2
                
            fun main()
                val x = double("hello")
        """.trimIndent()

        val expected = """
            test.txt 5.20:- Got type String when expecting Int
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun arrayAccess() {
        val prog = """
            fun sum(a:Array<Int>)->Int
                var sum = 0
                var count =0
                while count < a.size
                    sum = sum + a[count]
                    count = count + 1
                return sum 
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION sum (Array<Int>)->Int
            . . DECL LOCALVAR sum Int
            . . . INTLIT 0 Int
            . . DECL LOCALVAR count Int
            . . . INTLIT 0 Int
            . . WHILE
            . . . BINARYOP < Bool
            . . . . IDENTIFIER LOCALVAR count Int
            . . . . MEMBERACCESS size Int
            . . . . . IDENTIFIER LOCALVAR a Array<Int>
            . . . ASSIGN
            . . . . IDENTIFIER LOCALVAR sum Int
            . . . . BINARYOP + Int
            . . . . . IDENTIFIER LOCALVAR sum Int
            . . . . . ARRAYACCESS Int
            . . . . . . IDENTIFIER LOCALVAR a Array<Int>
            . . . . . . IDENTIFIER LOCALVAR count Int
            . . . ASSIGN
            . . . . IDENTIFIER LOCALVAR count Int
            . . . . BINARYOP + Int
            . . . . . IDENTIFIER LOCALVAR count Int
            . . . . . INTLIT 1 Int
            . . RETURN
            . . . IDENTIFIER LOCALVAR sum Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun arrayErrorIndex() {
        val prog = """
            fun foo()->Int
                return x[4]    # Should give an error for undefined symbol
                               # But no errors for indexing into error,
                               # or type error on return 
        """.trimIndent()

        val expected = """
            test.txt 2.12:- Undefined identifier: x
            """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun notArrayIndexError() {
        val prog = """
            fun foo()->Int
                return 6[4] 
        """.trimIndent()

        val expected = """
            test.txt 2.12:- Cannot index into type 'Int'
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun functionNoReturn() {
        val prog = """
            fun foo()->Int
                return
                
        """.trimIndent()

        val expected = """
            test.txt 2.5:- Function should return a value of type Int
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun undefinedTypeName() {
        val prog = """
            fun foo(x:fred)->Int
                return 4
        """.trimIndent()

        val expected = """
            test.txt 1.11:- Undefined identifier: fred
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun valueAsType() {
        val prog = """
            fun foo(x:Int)->Int
                val y:x = 5
                return y
        """.trimIndent()

        val expected = """
            test.txt 2.11:- x is not a type
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun classMembers() {
        val prog = """
            class Cat(val name:String, var age:Int)
                val legs = 4
                
            fun main()->Int
                val cat = Cat("Fluffy", 2)
                return cat.legs + cat.age
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Cat
            . . PARAMETER FIELD name String
            . . PARAMETER FIELD age Int
            . . DECL FIELD legs Int
            . . . INTLIT 4 Int
            . FUNCTION main ()->Int
            . . DECL LOCALVAR cat Cat
            . . . CONSTRUCTOR Cat
            . . . . IDENTIFIER TYPENAME Cat Cat
            . . . . STRINGLIT Fluffy String
            . . . . INTLIT 2 Int
            . . RETURN
            . . . BINARYOP + Int
            . . . . MEMBERACCESS legs Int
            . . . . . IDENTIFIER LOCALVAR cat Cat
            . . . . MEMBERACCESS age Int
            . . . . . IDENTIFIER LOCALVAR cat Cat

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun constructorNonClass() {
        val prog = """
            fun foo()
                val x = Int(4)
        """.trimIndent()

        val expected = """
            test.txt 2.13:- Cannot call constructor for type 'Int'
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun notClassMembers() {
        val prog = """
                
            fun main()->Int
                val cat = 5
                return cat.legs
        """.trimIndent()

        val expected = """
            test.txt 4.16:- Cannot access field legs of non-class type Int
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun badMemberName() {
        val prog = """
            class Cat(val name:String, var age:Int)
                val legs = 4
                
            fun main()->Int
                val cat = Cat("Fluffy", 2)
                return cat.arms + cat.age
        """.trimIndent()

        val expected = """
            test.txt 6.16:- Class 'Cat' has no field named 'arms'
        """.trimIndent()

        runTest(prog, expected)
    }



    @Test
    fun funcCallOnErrorSymbol() {
        val prog = """
            fun foo()
                val x = fred(4)   # should give error on undefined symbol,
                                  # but not on function call
        """.trimIndent()

        val expected = """
            test.txt 2.13:- Undefined identifier: fred
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun funcCallNotFunction() {
        val prog = """
            fun foo()
                val x = 6(4)
                
        """.trimIndent()

        val expected = """
            test.txt 2.13:- Got type 'Int' when expecting a function
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun funcCallAsStatement() {
        val prog = """
            fun foo()
                val x = 6
                
            fun main()
                foo()
                
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION foo ()->Unit
            . . DECL LOCALVAR x Int
            . . . INTLIT 6 Int
            . FUNCTION main ()->Unit
            . . EXPR
            . . . FUNCCALL Unit
            . . . . IDENTIFIER FUNC foo ()->Unit
            
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
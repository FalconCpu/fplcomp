import frontend.Lexer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class PathContextTest {

    private fun runTest(prog: String, expected: String) {
        val lexer = Lexer("test.txt", StringReader(prog))
        val output = compile(listOf(lexer), StopAt.TYPECHECK)
        assertEquals(expected, output)
    }

    @Test
    fun uninitializedVariable() {
        val prog = """
            fun main()->Int
                val x : Int
                return x
        """.trimIndent()

        val expected = """
            test.txt 3.12:- Variable 'x' has not been initialized
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun initializingVal() {
        val prog = """
            fun main()->Int
                val x : Int
                x = 5
                return x
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main ()->Int
            . . DECL LOCALVAR x Int
            . . ASSIGN
            . . . IDENTIFIER LOCALVAR x Int
            . . . INTLIT 5 Int
            . . RETURN
            . . . IDENTIFIER LOCALVAR x Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun assigningVal() {
        val prog = """
            fun main()->Int
                val x : Int
                x = 5
                x = 6      # error as reassigning val
                return x
        """.trimIndent()

        val expected = """
            test.txt 4.5:- Local variable x is not mutable
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun assigningVar() {
        val prog = """
            fun main()->Int
                var x : Int
                x = 5
                x = 6      # no error as reassigning var
                return x
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main ()->Int
            . . DECL LOCALVAR x Int
            . . ASSIGN
            . . . IDENTIFIER LOCALVAR x Int
            . . . INTLIT 5 Int
            . . ASSIGN
            . . . IDENTIFIER LOCALVAR x Int
            . . . INTLIT 6 Int
            . . RETURN
            . . . IDENTIFIER LOCALVAR x Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun maybeUninitialized() {
        val prog = """
            fun main(a:Int)->Int
                val x : Int
                if a=0
                    x = 5
                return x     # error as x may not be initialized
        """.trimIndent()

        val expected = """
            test.txt 5.12:- Variable 'x' might not be initialized
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun maybeUninitialized2() {
        val prog = """
            fun main(a:Int)->Int
                val x : Int
                if a=0
                    x = 5
                x = 6      # error as reassigning val
                return x
        """.trimIndent()

        val expected = """
            test.txt 5.5:- Local variable x is not mutable
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun returnMakesUnreachable() {
        val prog = """
            fun main(a:Int)->Int
                val x : Int
                if a=0
                    x = 5
                    return x
                x = 6      # No error as x is still uninitialized if code reaches here
                return x
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main (Int)->Int
            . . DECL LOCALVAR x Int
            . . IF
            . . . CLAUSE
            . . . . EQ Bool
            . . . . . IDENTIFIER LOCALVAR a Int
            . . . . . INTLIT 0 Int
            . . . . ASSIGN
            . . . . . IDENTIFIER LOCALVAR x Int
            . . . . . INTLIT 5 Int
            . . . . RETURN
            . . . . . IDENTIFIER LOCALVAR x Int
            . . ASSIGN
            . . . IDENTIFIER LOCALVAR x Int
            . . . INTLIT 6 Int
            . . RETURN
            . . . IDENTIFIER LOCALVAR x Int

        """.trimIndent()

        runTest(prog, expected)
    }



    @Test
    fun initializedInBothBranches() {
        val prog = """
            fun main(a:Int)->Int
                val x : Int
                if a=0
                    x = 5
                else
                    x = 6
                return x     # OK - as initialized in both branches
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main (Int)->Int
            . . DECL LOCALVAR x Int
            . . IF
            . . . CLAUSE
            . . . . EQ Bool
            . . . . . IDENTIFIER LOCALVAR a Int
            . . . . . INTLIT 0 Int
            . . . . ASSIGN
            . . . . . IDENTIFIER LOCALVAR x Int
            . . . . . INTLIT 5 Int
            . . . CLAUSE
            . . . . ASSIGN
            . . . . . IDENTIFIER LOCALVAR x Int
            . . . . . INTLIT 6 Int
            . . RETURN
            . . . IDENTIFIER LOCALVAR x Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun missingReturnInSomePath() {
        val prog = """
            fun main(a:Int)->Int
                if a=0
                    return 5
                # should be an error here as code can fall off the end without returning Int
        """.trimIndent()

        val expected = """
            test.txt 4.81:- Function should return a value of type Int
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun returnInBothPaths() {
        val prog = """
            fun main(a:Int)->Int
                if a=0
                    return 5
                else
                    return 6    
                # No error here as both paths return Int
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main (Int)->Int
            . . IF
            . . . CLAUSE
            . . . . EQ Bool
            . . . . . IDENTIFIER LOCALVAR a Int
            . . . . . INTLIT 0 Int
            . . . . RETURN
            . . . . . INTLIT 5 Int
            . . . CLAUSE
            . . . . RETURN
            . . . . . INTLIT 6 Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun nullableType() {
        val prog = """
            class Cat(val name:String, val age:Int)
            
            fun foo(c:Cat?)->Int
                return c.age    # Error as c may be null
        """.trimIndent()

        val expected = """
            test.txt 4.14:- Member access is not allowed on nullable type Cat?
        """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun nullTypeAccess() {
        val prog = """
            class Cat(val name:String, val age:Int)
            
            fun foo(c:Cat?)->Int
                if c!=null
                    return c.age    # no error as c has been checked for null
                else
                    return 0
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Cat
            . . PARAMETER FIELD name String
            . . PARAMETER FIELD age Int
            . FUNCTION foo (Cat?)->Int
            . . IF
            . . . CLAUSE
            . . . . NEQ Bool
            . . . . . IDENTIFIER LOCALVAR c Cat?
            . . . . . IDENTIFIER LITERAL null Null
            . . . . RETURN
            . . . . . MEMBERACCESS age Int
            . . . . . . IDENTIFIER LOCALVAR c Cat
            . . . CLAUSE
            . . . . RETURN
            . . . . . INTLIT 0 Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun nullTypeAccess2() {
        val prog = """
            class Cat(val name:String, val age:Int)
            
            fun foo(c:Cat?)->Int
                if c=null
                    return 0
                return c.age    # no error as c has been checked for null
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Cat
            . . PARAMETER FIELD name String
            . . PARAMETER FIELD age Int
            . FUNCTION foo (Cat?)->Int
            . . IF
            . . . CLAUSE
            . . . . EQ Bool
            . . . . . IDENTIFIER LOCALVAR c Cat?
            . . . . . IDENTIFIER LITERAL null Null
            . . . . RETURN
            . . . . . INTLIT 0 Int
            . . RETURN
            . . . MEMBERACCESS age Int
            . . . . IDENTIFIER LOCALVAR c Cat

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun nullTypeAccess3() {
        val prog = """
            class Cat(val name:String, val age:Int)
            
            fun foo(c:Cat?)->Int
                if null=c
                    return 0
                return c.age    # no error as c has been checked for null
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Cat
            . . PARAMETER FIELD name String
            . . PARAMETER FIELD age Int
            . FUNCTION foo (Cat?)->Int
            . . IF
            . . . CLAUSE
            . . . . EQ Bool
            . . . . . IDENTIFIER LITERAL null Null
            . . . . . IDENTIFIER LOCALVAR c Cat?
            . . . . RETURN
            . . . . . INTLIT 0 Int
            . . RETURN
            . . . MEMBERACCESS age Int
            . . . . IDENTIFIER LOCALVAR c Cat

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun nullTypeAccess4() {
        val prog = """
            class Cat(val name:String, val age:Int)
            
            fun foo(c:Cat?)->Int
                if null!=c
                    return 0    
                return c.age    # error - c is guaranteed to be null
        """.trimIndent()

        val expected = """
            test.txt 6.14:- Cannot access field age of non-class type Null
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun comparisonAccess1() {
        val prog = """
            class Cat(val name:String, val age:Int)
            
            fun foo(c:Cat?, d:Cat)->Int
                if c=d
                    return c.age  # OK as c compared to non-null d     
                return 0
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Cat
            . . PARAMETER FIELD name String
            . . PARAMETER FIELD age Int
            . FUNCTION foo (Cat?,Cat)->Int
            . . IF
            . . . CLAUSE
            . . . . EQ Bool
            . . . . . IDENTIFIER LOCALVAR c Cat?
            . . . . . IDENTIFIER LOCALVAR d Cat
            . . . . RETURN
            . . . . . MEMBERACCESS age Int
            . . . . . . IDENTIFIER LOCALVAR c Cat
            . . RETURN
            . . . INTLIT 0 Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun comparisonAccess2() {
        val prog = """
            class Cat(val name:String, val age:Int)
            
            fun foo(c:Cat?, d:Cat?)->Int
                if c=d
                    return c.age  # Not ok as c and d could both be null     
                return 0
        """.trimIndent()

        val expected = """
            test.txt 5.18:- Member access is not allowed on nullable type Cat?
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun looseSmartcast() {
        val prog = """
            class Cat(val name:String, val age:Int)
            
            fun foo(c:Cat?, d:Cat?)->Int
                var x = c
                if x=null
                    return 0
                val y = x.name     # Ok as has been null checked
                x = d
                val z = x.name     # not OK as x has been mutated since null check
                return 0
        """.trimIndent()

        val expected = """
            test.txt 9.15:- Member access is not allowed on nullable type Cat?
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun assignSmartCast() {
        val prog = """
            class Cat(val name:String, val age:Int)
            
            fun foo()->Int
                var c : Cat? = null
                val d = 1
                c = Cat("Fred", 10)
                return c.age         # OK as c has been reassigned
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Cat
            . . PARAMETER FIELD name String
            . . PARAMETER FIELD age Int
            . FUNCTION foo ()->Int
            . . DECL LOCALVAR c Cat?
            . . . IDENTIFIER LITERAL null Null
            . . DECL LOCALVAR d Int
            . . . INTLIT 1 Int
            . . ASSIGN
            . . . IDENTIFIER LOCALVAR c Cat?
            . . . CONSTRUCTOR Cat
            . . . . IDENTIFIER TYPENAME Cat Cat
            . . . . STRINGLIT Fred String
            . . . . INTLIT 10 Int
            . . RETURN
            . . . MEMBERACCESS age Int
            . . . . IDENTIFIER LOCALVAR c Cat

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun andSmartCast() {
        val prog = """
            class Cat(val name:String, val age:Int)
            
            fun foo(c:Cat?, d:Int)->Int
                if c!=null and d>0
                    return c.age  # OK as c compared to non-null even with and
                return 0
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Cat
            . . PARAMETER FIELD name String
            . . PARAMETER FIELD age Int
            . FUNCTION foo (Cat?,Int)->Int
            . . IF
            . . . CLAUSE
            . . . . AND Bool
            . . . . . NEQ Bool
            . . . . . . IDENTIFIER LOCALVAR c Cat?
            . . . . . . IDENTIFIER LITERAL null Null
            . . . . . COMPARE > Bool
            . . . . . . IDENTIFIER LOCALVAR d Int
            . . . . . . INTLIT 0 Int
            . . . . RETURN
            . . . . . MEMBERACCESS age Int
            . . . . . . IDENTIFIER LOCALVAR c Cat
            . . RETURN
            . . . INTLIT 0 Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun orSmartCast() {
        val prog = """
            class Cat(val name:String, val age:Int)
            
            fun foo(c:Cat?, d:Int)->Int
                if c!=null or d>0
                    return c.age  # Not ok as c could be null if d>0
                return 0
        """.trimIndent()

        val expected = """
            test.txt 5.18:- Member access is not allowed on nullable type Cat?
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun smartCastThroughMemberAccess() {
        val prog = """
            class LinkedListNode(val value:Int, val next:LinkedListNode?)
            
            fun nextValue(a:LinkedListNode)->Int
                if a.next!=null
                    return a.next.value    # this is OK as we have checked a.next is not null
                else
                    return 0
        """.trimIndent()

        val expected = """
            TOP
            . CLASS LinkedListNode
            . . PARAMETER FIELD value Int
            . . PARAMETER FIELD next LinkedListNode?
            . FUNCTION nextValue (LinkedListNode)->Int
            . . IF
            . . . CLAUSE
            . . . . NEQ Bool
            . . . . . MEMBERACCESS next LinkedListNode?
            . . . . . . IDENTIFIER LOCALVAR a LinkedListNode
            . . . . . IDENTIFIER LITERAL null Null
            . . . . RETURN
            . . . . . MEMBERACCESS value Int
            . . . . . . MEMBERACCESS next LinkedListNode
            . . . . . . . IDENTIFIER LOCALVAR a LinkedListNode
            . . . CLAUSE
            . . . . RETURN
            . . . . . INTLIT 0 Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun smartCastThroughMemberAccess2() {
        val prog = """
            class LinkedListNode(val value:Int, val next:LinkedListNode?)
            
            fun nextValue(a:LinkedListNode)->Int
                return a.next.value    # this should give an error as a.next could be null
        """.trimIndent()

        val expected = """
            test.txt 4.19:- Member access is not allowed on nullable type LinkedListNode?
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun smartCastThroughMemberAccess3() {
        val prog = """
            class LinkedListNode(val value:Int, val next:LinkedListNode?)
            
            fun nextValue(a:LinkedListNode)->Int
                var x = a
                if x.next!=null
                    return x.next.value    # error as x is a var so we can't smart cast to LinkedListNode
                else
                    return 0
        """.trimIndent()

        val expected = """
            test.txt 6.23:- Member access is not allowed on nullable type LinkedListNode?
        """.trimIndent()
        runTest(prog, expected)
    }

    @Test
    fun smartCastThroughMemberAccess4() {
        val prog = """
            class LinkedListNode(val value:Int, val next:LinkedListNode?)
            
            fun nextValue(a:LinkedListNode)->Int
                a.next = LinkedListNode(10, null)
                return a.next.value
        """.trimIndent()

        val expected = """
            TOP
            . CLASS LinkedListNode
            . . PARAMETER FIELD value Int
            . . PARAMETER FIELD next LinkedListNode?
            . FUNCTION nextValue (LinkedListNode)->Int
            . . ASSIGN
            . . . MEMBERACCESS next LinkedListNode?
            . . . . IDENTIFIER LOCALVAR a LinkedListNode
            . . . CONSTRUCTOR LinkedListNode
            . . . . IDENTIFIER TYPENAME LinkedListNode LinkedListNode
            . . . . INTLIT 10 Int
            . . . . IDENTIFIER LITERAL null Null
            . . RETURN
            . . . MEMBERACCESS value Int
            . . . . MEMBERACCESS next LinkedListNode
            . . . . . IDENTIFIER LOCALVAR a LinkedListNode

        """.trimIndent()
        runTest(prog, expected)
    }

    @Test
    fun ifExpressionTest() {
        val prog = """
            class Cat(val name:String, val age:Int)

            fun main(c:Cat?)->String
                return if c!=null then c.name else "unknown"
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Cat
            . . PARAMETER FIELD name String
            . . PARAMETER FIELD age Int
            . FUNCTION main (Cat?)->String
            . . RETURN
            . . . IF_EXPR String
            . . . . NEQ Bool
            . . . . . IDENTIFIER LOCALVAR c Cat?
            . . . . . IDENTIFIER LITERAL null Null
            . . . . MEMBERACCESS name String
            . . . . . IDENTIFIER LOCALVAR c Cat
            . . . . STRINGLIT unknown String

        """.trimIndent()

        runTest(prog, expected)
    }



}
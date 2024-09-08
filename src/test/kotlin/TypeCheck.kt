import frontend.Lexer
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
            . . BINARYOP add Int
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
            fun count(a:Int)
                var sum = 0
                var count =0
                while count < a
                    sum = sum + count
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION count(Int)->Unit
            . . DECL LOCALVAR sum Int
            . . . INTLIT 0 Int
            . . DECL LOCALVAR count Int
            . . . INTLIT 0 Int
            . . WHILE
            . . . COMPARE clt Bool
            . . . . IDENTIFIER LOCALVAR count Int
            . . . . IDENTIFIER LOCALVAR a Int
            . . . ASSIGN
            . . . . IDENTIFIER LOCALVAR sum Int
            . . . . BINARYOP add Int
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
            . FUNCTION double(Int)->Int
            . . RETURN
            . . . BINARYOP mul Int
            . . . . IDENTIFIER LOCALVAR a Int
            . . . . INTLIT 2 Int
            . FUNCTION main()->Unit
            . . DECL LOCALVAR x Int
            . . . FUNCCALL double(Int) Int
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
            test.txt 5.13:- No functions match double(Int, Int). Possibilities are:-
                              double(Int)
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
            test.txt 5.13:- No functions match double(String). Possibilities are:-
                              double(Int)
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
            . FUNCTION sum(Array<Int>)->Int
            . . DECL LOCALVAR sum Int
            . . . INTLIT 0 Int
            . . DECL LOCALVAR count Int
            . . . INTLIT 0 Int
            . . WHILE
            . . . COMPARE clt Bool
            . . . . IDENTIFIER LOCALVAR count Int
            . . . . MEMBERACCESS size Int
            . . . . . IDENTIFIER LOCALVAR a Array<Int>
            . . . ASSIGN
            . . . . IDENTIFIER LOCALVAR sum Int
            . . . . BINARYOP add Int
            . . . . . IDENTIFIER LOCALVAR sum Int
            . . . . . ARRAYACCESS Int
            . . . . . . IDENTIFIER LOCALVAR a Array<Int>
            . . . . . . IDENTIFIER LOCALVAR count Int
            . . . ASSIGN
            . . . . IDENTIFIER LOCALVAR count Int
            . . . . BINARYOP add Int
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
            test.txt 2.5:- Function foo() should return a value of type Int
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
            . . DECL FIELD legs Int
            . . . INTLIT 4 Int
            . FUNCTION main()->Int
            . . DECL LOCALVAR cat Cat
            . . . CONSTRUCTOR Cat
            . . . . STRINGLIT Fluffy String
            . . . . INTLIT 2 Int
            . . RETURN
            . . . BINARYOP add Int
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
            test.txt 2.13:- Not a function to call
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
            . FUNCTION foo()->Unit
            . . DECL LOCALVAR x Int
            . . . INTLIT 6 Int
            . FUNCTION main()->Unit
            . . EXPR
            . . . FUNCCALL foo() Unit
            
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun globalVars() {
        val prog = """
            val x = 5
            fun main()->Int
                return x
        """.trimIndent()

        val expected = """
            TOP
            . DECL GLOBALVAR x Int
            . . INTLIT 5 Int
            . FUNCTION main()->Int
            . . RETURN
            . . . IDENTIFIER GLOBALVAR x Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun globalVarsNotMutable() {
        val prog = """
            val x = 5
            fun main()->Int
                x = 6
                return x
        """.trimIndent()

        val expected = """
            test.txt 3.5:- Global variable x is not mutable
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun classLocalVariable() {
        val prog = """
            class Cat(val name:String, age:Int)
                val old = age>10
                
            fun main()->Int
                val c = Cat("Fluffy", 2)
                return c.age              # error as age is not a field
        """.trimIndent()

        val expected = """
            test.txt 6.14:- Class 'Cat' has no field named 'age'
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun fieldAccess() {
        val prog = """
            class Cat(val name:String, var age:Int)
                fun timePasses()
                    age = age+1
                
            fun main()->Int
                val c = Cat("Fluffy", 2)
                c.timePasses()                
                return c.age
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Cat
            . . FUNCTION Cat/timePasses()->Unit
            . . . ASSIGN
            . . . . IDENTIFIER FIELD age Int
            . . . . BINARYOP add Int
            . . . . . IDENTIFIER FIELD age Int
            . . . . . INTLIT 1 Int
            . FUNCTION main()->Int
            . . DECL LOCALVAR c Cat
            . . . CONSTRUCTOR Cat
            . . . . STRINGLIT Fluffy String
            . . . . INTLIT 2 Int
            . . EXPR
            . . . FUNCCALL Cat/timePasses() Unit
            . . . . IDENTIFIER LOCALVAR c Cat
            . . RETURN
            . . . MEMBERACCESS age Int
            . . . . IDENTIFIER LOCALVAR c Cat

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun localVarAccessFromMemberFunc() {
        val prog = """
            class Cat(val name:String, age:Int)
                fun timePasses()
                    age = age+1         # error as age is not a field
                
            fun main()->Int
                val c = Cat("Fluffy", 2)
                c.timePasses()                
                return c.age
        """.trimIndent()

        val expected = """
            test.txt 3.9:- Undefined identifier: age
            test.txt 8.14:- Class 'Cat' has no field named 'age'
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun localVarInConstructor() {
        val prog = """
            class Cat(val name:String, age:Int)
                val days = age*365
                
            fun main()->Int
                val c = Cat("Fluffy", 2)
                return c.days
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Cat
            . . DECL FIELD days Int
            . . . BINARYOP mul Int
            . . . . IDENTIFIER LOCALVAR age Int
            . . . . INTLIT 365 Int
            . FUNCTION main()->Int
            . . DECL LOCALVAR c Cat
            . . . CONSTRUCTOR Cat
            . . . . STRINGLIT Fluffy String
            . . . . INTLIT 2 Int
            . . RETURN
            . . . MEMBERACCESS days Int
            . . . . IDENTIFIER LOCALVAR c Cat

        """.trimIndent()

        runTest(prog, expected)
    }




    @Test
    fun notTest() {
        val prog = """
            fun main(a:Bool)->Int
                if not a
                    return 1
                else
                    return 2
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main(Bool)->Int
            . . IF
            . . . CLAUSE
            . . . . NOT Bool
            . . . . . IDENTIFIER LOCALVAR a Bool
            . . . . RETURN
            . . . . . INTLIT 1 Int
            . . . CLAUSE
            . . . . RETURN
            . . . . . INTLIT 2 Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun negTest() {
        val prog = """
            fun main(a:Int)->Int
                return -a
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main(Int)->Int
            . . RETURN
            . . . NEG Int
            . . . . IDENTIFIER LOCALVAR a Int

            """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun negRealTest() {
        val prog = """
            fun main(a:Real)->Real
                return -a
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main(Real)->Real
            . . RETURN
            . . . NEG Real
            . . . . IDENTIFIER LOCALVAR a Real

        """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun negStringTest() {
        val prog = """
            fun main(a:String)->String
                return -a
        """.trimIndent()

        val expected = """
            test.txt 2.12:- No operation defined for unary minus String
        """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun badTypeAsValue() {
        val prog = """
            fun main()
                val x = Int

        """.trimIndent()

        val expected = """
            test.txt 2.13:- Got type name 'Int' when expecting a value
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun compareBadType() {
        val prog = """
            fun main(x:Int) -> Int
                if x = "hello"
                    return 1
                else    
                    return 2

        """.trimIndent()

        val expected = """
            test.txt 2.10:- Equality check of incompatible types: Int and String
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun elvisTest() {
        val prog = """
            class Cat(val name:String?, age:Int)
            
            fun getName(c:Cat)->String
                return c.name ?: "Unknown"
            
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Cat
            . FUNCTION getName(Cat)->String
            . . RETURN
            . . . ELVIS String
            . . . . MEMBERACCESS name String?
            . . . . . IDENTIFIER LOCALVAR c Cat
            . . . . STRINGLIT Unknown String

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun elvisNotNullableTest() {
        val prog = """
            class Cat(val name:String, age:Int)
            
            fun getName(c:Cat)->String
                return c.name ?: "Unknown"
            
        """.trimIndent()

        val expected = """
            test.txt 4.14:- Not a nullable type for elvis operator: String
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun elvisBadType() {
        val prog = """
            class Cat(val name:String?, age:Int)
            
            fun getName(c:Cat)->String
                return c.name ?: 54
            
        """.trimIndent()

        val expected = """
            test.txt 4.22:- Incompatible types for elvis operator String? and Int
        """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun ifExpressionTest() {
        val prog = """
            fun main(a:Int)->String
                return if a=0 then "zero" else "not zero"
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main(Int)->String
            . . RETURN
            . . . IF_EXPR String
            . . . . EQ Bool
            . . . . . IDENTIFIER LOCALVAR a Int
            . . . . . INTLIT 0 Int
            . . . . STRINGLIT zero String
            . . . . STRINGLIT not zero String

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun stringCompare() {
        val prog = """
            fun main(a:String, b:String)->String
                if a<b
                    return "a is less than b"
                else if a>b
                    return "a is greater than b"
                else
                    return "a is equal to b"
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main(String,String)->String
            . . IF
            . . . CLAUSE
            . . . . COMPARE clt Bool
            . . . . . IDENTIFIER LOCALVAR a String
            . . . . . IDENTIFIER LOCALVAR b String
            . . . . RETURN
            . . . . . STRINGLIT a is less than b String
            . . . CLAUSE
            . . . . COMPARE cgt Bool
            . . . . . IDENTIFIER LOCALVAR a String
            . . . . . IDENTIFIER LOCALVAR b String
            . . . . RETURN
            . . . . . STRINGLIT a is greater than b String
            . . . CLAUSE
            . . . . RETURN
            . . . . . STRINGLIT a is equal to b String

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun stringCompareError() {
        val prog = """
            fun main(a:String, b:String)->String
                if a<5
                    return "a is less than b"
                return "a is greater than b"
        """.trimIndent()

        val expected = """
            test.txt 2.9:- No operation defined for String < Int
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun realCompare() {
        val prog = """
            fun main(a:Real, b:Real)->String
                if a<b
                    return "a is less than b"
                else if a>b
                    return "a is greater than b"
                else
                    return "a is equal to b"
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main(Real,Real)->String
            . . IF
            . . . CLAUSE
            . . . . COMPARE clt Bool
            . . . . . IDENTIFIER LOCALVAR a Real
            . . . . . IDENTIFIER LOCALVAR b Real
            . . . . RETURN
            . . . . . STRINGLIT a is less than b String
            . . . CLAUSE
            . . . . COMPARE cgt Bool
            . . . . . IDENTIFIER LOCALVAR a Real
            . . . . . IDENTIFIER LOCALVAR b Real
            . . . . RETURN
            . . . . . STRINGLIT a is greater than b String
            . . . CLAUSE
            . . . . RETURN
            . . . . . STRINGLIT a is equal to b String

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun andOrTest() {
        val prog = """
            fun main(a:Int, b:Int)->String
                if a=1 and b=2 or a<0
                    return "a is 1 and b is 2"
                else
                    return "a is not 1 and b is not 2"
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main(Int,Int)->String
            . . IF
            . . . CLAUSE
            . . . . OR Bool
            . . . . . AND Bool
            . . . . . . EQ Bool
            . . . . . . . IDENTIFIER LOCALVAR a Int
            . . . . . . . INTLIT 1 Int
            . . . . . . EQ Bool
            . . . . . . . IDENTIFIER LOCALVAR b Int
            . . . . . . . INTLIT 2 Int
            . . . . . COMPARE clt Bool
            . . . . . . IDENTIFIER LOCALVAR a Int
            . . . . . . INTLIT 0 Int
            . . . . RETURN
            . . . . . STRINGLIT a is 1 and b is 2 String
            . . . CLAUSE
            . . . . RETURN
            . . . . . STRINGLIT a is not 1 and b is not 2 String

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun castTest() {
        val prog = """
            class Cat(val name:String, var age:Int)
            
            fun main()->Int
                val c = (123456:Cat)
                return c.age
        """.trimIndent()

        val expected = """  
            TOP
            . CLASS Cat
            . FUNCTION main()->Int
            . . DECL LOCALVAR c Cat
            . . . CAST Cat
            . . . . INTLIT 123456 Int
            . . RETURN
            . . . MEMBERACCESS age Int
            . . . . IDENTIFIER LOCALVAR c Cat

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun enumTest() {
        val prog = """
            enum Color (RED,GREEN,BLUE)
            
            fun main()->Color
                return Color.RED
        """.trimIndent()

        val expected = """
            TOP
            . ENUM Color
            . FUNCTION main()->Color
            . . RETURN
            . . . IDENTIFIER LITERAL RED Color

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun repeatTest() {
        val prog = """
            fun main()->Int
                var i=0
                repeat
                    i=i+1
                until i=10
                return i
        """.trimIndent()

        val expected = """  
            TOP
            . FUNCTION main()->Int
            . . DECL LOCALVAR i Int
            . . . INTLIT 0 Int
            . . REPEAT
            . . . EQ Bool
            . . . . IDENTIFIER LOCALVAR i Int
            . . . . INTLIT 10 Int
            . . . ASSIGN
            . . . . IDENTIFIER LOCALVAR i Int
            . . . . BINARYOP add Int
            . . . . . IDENTIFIER LOCALVAR i Int
            . . . . . INTLIT 1 Int
            . . RETURN
            . . . IDENTIFIER LOCALVAR i Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun arrayConstructor() {
        val prog = """
            fun main()->Array<Int>
                return Array<Int>(10)
        """.trimIndent()

        val expected = """  
            TOP
            . FUNCTION main()->Array<Int>
            . . RETURN
            . . . ARRAYCONSTRUCTOR Array<Int>
            . . . . INTLIT 10 Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun arrayLiteral() {
        val prog = """
            fun main()->Array<Int>
                return arrayOf(1,2,3)
        """.trimIndent()

        val expected = """  
            TOP
            . FUNCTION main()->Array<Int>
            . . RETURN
            . . . ARRAYOF Array<Int>
            . . . . INTLIT 1 Int
            . . . . INTLIT 2 Int
            . . . . INTLIT 3 Int

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun arrayLiteralTypeMismatch1() {
        val prog = """
            fun main()->Array<Int>
                return arrayOf(1,2,"3")
        """.trimIndent()

        val expected = """  
            test.txt 2.24:- Got type String when expecting Int
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun arrayLiteralTypeMismatch2() {
        val prog = """
            fun main()->Array<Int>
                return arrayOf<String>(1,2,3)
        """.trimIndent()

        val expected = """  
            test.txt 2.28:- Got type Int when expecting String
            test.txt 2.30:- Got type Int when expecting String
            test.txt 2.32:- Got type Int when expecting String
            test.txt 2.5:- Got type Array<String> when expecting Array<Int>
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun helloWorld() {
        val prog = """
            fun main()
                println "Hello, world!"
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION main()->Unit
            . . PRINT
            . . . STRINGLIT Hello, world! String

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
            TOP
            . FUNCTION main()->Unit
            . . FOR_RANGE i
            . . . INTLIT 1 Int
            . . . INTLIT 10 Int
            . . . PRINT
            . . . . IDENTIFIER LOCALVAR i Int

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
            TOP
            . CONST TWO
            . . INTLIT 2 Int
            . FUNCTION main()->Unit
            . . PRINT
            . . . IDENTIFIER LITERAL TWO Int

        """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun writeToConstArray() {
        val prog = """
            fun main()
                val a = arrayOf(1,2,3)
                a[1] = 4        # Error: cannot assign to const array
        """.trimIndent()

        val expected = """
            test.txt 3.5:- Cannot write to immutable array
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun tupleTest() {
        val prog = """
            fun foo(a:Int,b:Int)->(Int,Int)
                return b,a
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION foo(Int,Int)->(Int, Int)
            . . RETURN
            . . . TUPLE
            . . . . IDENTIFIER LOCALVAR b Int
            . . . . IDENTIFIER LOCALVAR a Int

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
             test.txt 1.10:- Tuple parameters not supported
        """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun pointerTest() {
        val prog = """
            fun foo(a:Pointer<Int>, b:Pointer)->Int
                return a[2]
        """.trimIndent()

        val expected = """
            TOP
            . FUNCTION foo(Pointer<Int>,Pointer)->Int
            . . RETURN
            . . . ARRAYACCESS Int
            . . . . IDENTIFIER LOCALVAR a Pointer<Int>
            . . . . INTLIT 2 Int

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
            TOP
            . FUNCTION foo(Pointer<Int>?)->Int
            . . DECL LOCALVAR b Pointer<Int>
            . . . NULLCHECK Pointer<Int>
            . . . . IDENTIFIER LOCALVAR a Pointer<Int>?
            . . RETURN
            . . . ARRAYACCESS Int
            . . . . IDENTIFIER LOCALVAR a Pointer<Int>
            . . . . INTLIT 2 Int

        """.trimIndent()

        runTest(prog, expected)
    }


}
import falcon.Lexer
import falcon.StopAt
import falcon.compile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.StringReader

class InheritanceTest {

    private fun runTest(prog: String, expected: String) {
        val lexer = Lexer("test.txt", StringReader(prog))
        val output = compile(listOf(lexer), StopAt.TYPECHECK)
        assertEquals(expected, output)
    }

    @Test
    fun classDefinitionUndefinedSupertype() {
        val prog = """
            class Dog(name:String, val owner:String) : Animal(name)

        """.trimIndent()

        val expected = """
            test.txt 1.44:- Unknown super class Animal
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun classDefinitionBadSupertype() {
        val prog = """
            class Dog(name:String, val owner:String) : Int(43)

        """.trimIndent()

        val expected = """
            test.txt 1.44:- Super class Int is not a class
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun classDefinition() {
        val prog = """
            class Animal(val name:String)
            class Dog(name:String, val owner:String) : Animal(name)
            
            fun main()
                val a : Animal = Dog("Fido", "Simon")   # Allowed as Dog is a subclass of Animal
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Animal
            . . PARAMETER FIELD name String
            . CLASS Dog
            . . PARAMETER LOCALVAR name String
            . . PARAMETER FIELD owner String
            . . SUPERCLASS Animal
            . . . IDENTIFIER LOCALVAR name String
            . FUNCTION main ()->Unit
            . . DECL LOCALVAR a Animal
            . . . CONSTRUCTOR Dog
            . . . . IDENTIFIER TYPENAME Dog Dog
            . . . . STRINGLIT Fido String
            . . . . STRINGLIT Simon String
            
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun classDefinitionError() {
        val prog = """
            class Animal(val name:String)
            class Dog(name:String, val owner:String) : Animal(name)
            
            fun main()
                val d : Dog = Animal("Fido")    # Error as Animal is not a subclass of Dog
        """.trimIndent()

        val expected = """
            test.txt 5.19:- Got type Animal when expecting Dog
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun memberAccess() {
        val prog = """
            class Animal(val name:String)
            
            class Dog(name:String, val owner:String) : Animal(name)
            
            fun main(d : Dog) -> String
                return d.name             # accessing the name field of the Animal superclass
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Animal
            . . PARAMETER FIELD name String
            . CLASS Dog
            . . PARAMETER LOCALVAR name String
            . . PARAMETER FIELD owner String
            . . SUPERCLASS Animal
            . . . IDENTIFIER LOCALVAR name String
            . FUNCTION main (Dog)->String
            . . RETURN
            . . . MEMBERACCESS name String
            . . . . IDENTIFIER LOCALVAR d Dog

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun accessSubclassField() {
        val prog = """
            class Animal(val name:String)
            class Dog(name:String, val owner:String) : Animal(name)
            
            fun main(a: Animal) -> String
                return a.owner    # Error as owner field of the Dog subclass
        """.trimIndent()

        val expected = """
            test.txt 5.14:- Class 'Animal' has no field named 'owner'
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun accessSubclassFieldSmartCast() {
        val prog = """
            class Animal(val name:String)
            class Dog(name:String, val owner:String) : Animal(name)
            
            fun main(a: Animal) -> String
                if (a is Dog)
                    return a.owner    # OK as we smart cast to Dog
                else
                    return "Not a dog"
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Animal
            . . PARAMETER FIELD name String
            . CLASS Dog
            . . PARAMETER LOCALVAR name String
            . . PARAMETER FIELD owner String
            . . SUPERCLASS Animal
            . . . IDENTIFIER LOCALVAR name String
            . FUNCTION main (Animal)->String
            . . IF
            . . . CLAUSE
            . . . . IS Dog
            . . . . . IDENTIFIER LOCALVAR a Animal
            . . . . RETURN
            . . . . . MEMBERACCESS owner String
            . . . . . . IDENTIFIER LOCALVAR a Dog
            . . . CLAUSE
            . . . . RETURN
            . . . . . STRINGLIT Not a dog String

        """.trimIndent()

        runTest(prog, expected)
    }


    @Test
    fun accessSubclassFieldSmartCast2() {
        val prog = """
            class Animal(val name:String)
            class Dog(name:String, val owner:String) : Animal(name)
            
            fun main(a: Animal) -> String
                if (a isnot Dog)
                    return "Not a dog"
                return a.owner    # OK as any non-dog has been filtered out
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Animal
            . . PARAMETER FIELD name String
            . CLASS Dog
            . . PARAMETER LOCALVAR name String
            . . PARAMETER FIELD owner String
            . . SUPERCLASS Animal
            . . . IDENTIFIER LOCALVAR name String
            . FUNCTION main (Animal)->String
            . . IF
            . . . CLAUSE
            . . . . IS Dog
            . . . . . IDENTIFIER LOCALVAR a Animal
            . . . . RETURN
            . . . . . STRINGLIT Not a dog String
            . . RETURN
            . . . MEMBERACCESS owner String
            . . . . IDENTIFIER LOCALVAR a Dog

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun accessSubclassFieldSmartCast3() {
        val prog = """
            class Animal(val name:String)
            class Dog(name:String, val owner:String) : Animal(name)
            
            fun main(a: Animal, b:Int) -> String
                if (a is Dog or b = 2)
                    return a.owner    # error as b might not be a dog
                return "Not a dog"
        """.trimIndent()

        val expected = """
            test.txt 6.18:- Class 'Animal' has no field named 'owner'
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun accessSubclassFieldSmartCast4() {
        val prog = """
            class Animal(val name:String)
            class Dog(name:String, val owner:String) : Animal(name)
            
            fun main(a: Animal, b:Int) -> String
                if (a is Dog and b = 2)
                    return a.owner    # error as b might not be a dog
                return "Not a dog"
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Animal
            . . PARAMETER FIELD name String
            . CLASS Dog
            . . PARAMETER LOCALVAR name String
            . . PARAMETER FIELD owner String
            . . SUPERCLASS Animal
            . . . IDENTIFIER LOCALVAR name String
            . FUNCTION main (Animal,Int)->String
            . . IF
            . . . CLAUSE
            . . . . AND Bool
            . . . . . IS Dog
            . . . . . . IDENTIFIER LOCALVAR a Animal
            . . . . . EQ Bool
            . . . . . . IDENTIFIER LOCALVAR b Int
            . . . . . . INTLIT 2 Int
            . . . . RETURN
            . . . . . MEMBERACCESS owner String
            . . . . . . IDENTIFIER LOCALVAR a Dog
            . . RETURN
            . . . STRINGLIT Not a dog String

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun accessSubclassFieldSmartCast5() {
        val prog = """
            class Animal(val name:String)
            class Dog(name:String, val owner:String) : Animal(name)
            
            fun main(a: Animal?) -> String
                if (a is Dog)
                    return a.owner    # OK as we have qualified that a is a dog
                return "Not a dog"
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Animal
            . . PARAMETER FIELD name String
            . CLASS Dog
            . . PARAMETER LOCALVAR name String
            . . PARAMETER FIELD owner String
            . . SUPERCLASS Animal
            . . . IDENTIFIER LOCALVAR name String
            . FUNCTION main (Animal?)->String
            . . IF
            . . . CLAUSE
            . . . . IS Dog
            . . . . . IDENTIFIER LOCALVAR a Animal?
            . . . . RETURN
            . . . . . MEMBERACCESS owner String
            . . . . . . IDENTIFIER LOCALVAR a Dog
            . . RETURN
            . . . STRINGLIT Not a dog String

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun memberFunctions() {
        val prog = """
            class Animal(val name:String)
                fun speak() -> String
                    return "I am an animal"
            
            class Dog(name:String, val owner:String) : Animal(name)
            
            fun main(d : Dog) -> String
                return d.speak()
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Animal
            . . PARAMETER FIELD name String
            . . FUNCTION speak ()->String
            . . . RETURN
            . . . . STRINGLIT I am an animal String
            . CLASS Dog
            . . PARAMETER LOCALVAR name String
            . . PARAMETER FIELD owner String
            . . SUPERCLASS Animal
            . . . IDENTIFIER LOCALVAR name String
            . FUNCTION main (Dog)->String
            . . RETURN
            . . . FUNCCALL String
            . . . . MEMBERACCESS speak ()->String
            . . . . . IDENTIFIER LOCALVAR d Dog

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun overrideClosedFunctions() {
        val prog = """
            class Animal(val name:String)
                fun speak() -> String
                    return "I am an animal"
            
            class Dog(name:String, val owner:String) : Animal(name)
                override fun speak() -> String
                    return "I am a dog"
            
            fun main(d : Dog) -> String
                return d.speak()
        """.trimIndent()

        val expected = """
            test.txt 6.18:- Cannot override closed method
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun overrideFunctions() {
        val prog = """
            class Animal(val name:String)
                open fun speak() -> String
                    return "I am an animal"
            
            class Dog(name:String, val owner:String) : Animal(name)
                override fun speak() -> String
                    return "I am a dog"
            
            fun main(d : Dog) -> String
                return d.speak()
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Animal
            . . PARAMETER FIELD name String
            . . FUNCTION speak ()->String
            . . . RETURN
            . . . . STRINGLIT I am an animal String
            . CLASS Dog
            . . PARAMETER LOCALVAR name String
            . . PARAMETER FIELD owner String
            . . SUPERCLASS Animal
            . . . IDENTIFIER LOCALVAR name String
            . . FUNCTION speak ()->String
            . . . RETURN
            . . . . STRINGLIT I am a dog String
            . FUNCTION main (Dog)->String
            . . RETURN
            . . . FUNCCALL String
            . . . . MEMBERACCESS speak ()->String
            . . . . . IDENTIFIER LOCALVAR d Dog

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun abstractClass() {
        val prog = """
            abstract class Animal(val name:String)
                open fun speak() -> String
                    return "I am an animal"
            
            class Dog(name:String, val owner:String) : Animal(name)
                override fun speak() -> String
                    return "I am a dog"
            
            fun main()
                val a = Animal("Fred")     # error as instantiating an abstract class
                a.speak()
        """.trimIndent()

        val expected = """
            test.txt 10.13:- Cannot call constructor for abstract class 'Animal'
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun abstractMethod() {
        val prog = """
            abstract class Animal(val name:String)
                abstract fun speak() -> String
            
            class Dog(name:String, val owner:String) : Animal(name)
                override fun speak() -> String
                    return "I am a dog"
            
            fun main()
                val a = Dog("Fred","Simon")
                a.speak()
        """.trimIndent()

        val expected = """
            TOP
            . CLASS Animal
            . . PARAMETER FIELD name String
            . . FUNCTION speak ()->String
            . CLASS Dog
            . . PARAMETER LOCALVAR name String
            . . PARAMETER FIELD owner String
            . . SUPERCLASS Animal
            . . . IDENTIFIER LOCALVAR name String
            . . FUNCTION speak ()->String
            . . . RETURN
            . . . . STRINGLIT I am a dog String
            . FUNCTION main ()->Unit
            . . DECL LOCALVAR a Dog
            . . . CONSTRUCTOR Dog
            . . . . IDENTIFIER TYPENAME Dog Dog
            . . . . STRINGLIT Fred String
            . . . . STRINGLIT Simon String
            . . EXPR
            . . . FUNCCALL String
            . . . . MEMBERACCESS speak ()->String
            . . . . . IDENTIFIER LOCALVAR a Dog

        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun missingAbstractMethod() {
        val prog = """
            abstract class Animal(val name:String)
                abstract fun speak() -> String
            
            class Dog(name:String, val owner:String) : Animal(name)
            
            fun main()
                val a = Dog("Fred","Simon")
                a.speak()
        """.trimIndent()

        val expected = """
            test.txt 2.18:- No override provided for abstract function 'speak'
        """.trimIndent()

        runTest(prog, expected)
    }

    @Test
    fun abstractFunctionNotInAbstractClass() {
        val prog = """
            class Animal(val name:String)
                abstract fun speak() -> String     # error as abstract function not in abstract class
            
            class Dog(name:String, val owner:String) : Animal(name)
            
            fun main()
                val a = Dog("Fred","Simon")
                a.speak()
        """.trimIndent()

        val expected = """
            test.txt 2.14:- Cannot have abstract method outside abstract class
        """.trimIndent()

        runTest(prog, expected)
    }





}
# Falcon Programming Language

## Introduction

As my retirement project I am attempting to create a computer system from scratch. 
So far I have designed my own cpu (named imaginatively enough F32), running on a 
CycloneV FPGA. The CPU is based loosely on Risc-V.

To this I have added a memory system (64Mb of ram) and a VGA display (with blitter), 
and PS2 keyboard and mouse.

I then have a simple assembler, disassembler and system emulator.

The hardware project is here:   https://github.com/FalconCpu/falcon

The next step was to design a compiler to target this system. And I decided to have
a go at designing my own language. The document you are currently reading is my 
pitiful attempts at documenting it.

Please bear in mind this is all very experimental. There are lots of things I think 
might be a good idea, so I have a go at implementing them and then conclude
they are a bad idea. So everything is constantly in a state of flux.

## Language Design

My first goal for my language is to be able to write the operating system for the computer in it. 
This it has to be low level enough to be able to run on a bare CPU. But I also want it to be as 
ergonomic as possible, with those as many of the niceties of modern languages as possible.

Hence, the idea is the language will be strongly statically typed, with null safety and type inference.
But make it possible to escape from the type system where necessary. Initially the language will be
manually memory managed, but perhaps later I will look at adding reference counting or garbage collection - 
if I can do that in a way that doesn't break the ability to run bare metal code when needed.

The compiler compiles down to assembly - which can then be assembled and run using the tools in my hardware project.

## Language Features

I have borrowed heavily from other languages, mostly Kotlin, C and Python. The compiler itself is written in Kotlin.

* Imperative
* Static Typed, but with type inference
* Null safe -
* Manual memory management (no garbage collector)(although I want to explore ways to make it memory safe later)
* Python style significant indentation - but with optional end markers for blocks.

Let's start with an example program:

    # Comments start with a # and continue to the end of the line
    # functions begin with the 'fun' keyword
    # parameter types have the form name:Type, with the function return type shown afrer an arrow
    
    fun factorial(n:Int)->Int
        if n <= 1
            return 1
        else
            return n * factorial(n-1)

Hopefully nothing looks too surprising.                     

### Block structure

Blocks are marked by indentation (significant whitespace), but can optionally be terminated by an 'end' statement
this helps to mitigate the whitespace cliff seen in Python. The recommendation is short
blocks are marked using indentation only, longer blocks with explicit 'end'

### Variable declaration

Variables can be mutable (var) or immutable (val). Types are optional, if the type is not specified the compiler will
infer it from the initial assignment.

With immutable variables the compiler will give an error if there is any possible path under which the variable is 
assigned twice. The analysis is conservative and relatively superficial level - don't expect deep reasoning about
what is reachable. 

So for example the following code is valid:

    fun main(a:Int) -> String
        val x : String      # x is defined as an immutable holding a String value
        if a = 0
            x = "zero"
        else
            x = "not zero"
        return x

Although `x` is declared as immutable, the compiler is able to prove that along any path `x` is only assigned once. 

But the following code will give an error:

    fun main(a:Int) -> String
        val x : String      
        x = "zero"
        if a != 0
            x = "not zero"    # This will be flagged as an error - x is assigned twice
        return x

If you want to be able to reassign a variable, you can use a mutable variable (var)

    fun main(a:Int) -> String
        var x : String      
        x = "zero"
        if a != 0
            x = "not zero"    # This is fine
        return x

Variables must be initialized before they are used. So the following code will give an error:

    fun main(a:Int) -> String
        val x : String      
        if a != 0
            x = "not zero"
        return x              # This will be flagged as an error - x is potentially not initialized


### Expressions

All the normal operators are supported, with the following precedence:

1. Posfix operators  `.` `?.` `[]` `()` `!!` 
2. Prefix operators  `-` `not` `local`
3. Multiplicative `*` `/` `%` `&` `shl` `shr` `ushr`
4. Additive `+` `-` `|` `^`
5. Elvis `?:`
6. Comparison `<` `>` `<=` `>=` `=` `!=`
7. And  `and`
8. Or  `or`
9. if then `if then else`

I have used `=` to mean equality checking, rather than C's `==` because I think it is more readable. But I might change
ny mind on this. Similarly I have chosen to spell the short circuit operators as `and` `or` `not`
rather than C style `&&` `||` `!`

Currently, I don't support embedding assignment inside an expression.  

Also, I currently don't have `++` and `--` `+=` `-=` operators.  Although I might add them later.

`local` is used to indicate a value has meaning only within the current block, and will be automatically deleted when
the block exits.

### Null safety

The language has null safety (see later). The struct access operator `.` cannot be used when the compiler cannot
prove that the reference is not null. The `?.` is a null safe access operator, which will return null if the reference
is null.

The elvis operator `?:` evaluates the expression on its left, and if it is null, evaluates the expression on its right.
This, combined with the safe access operator, is a useful way to provide a default value for the case where the
value is null ( eg `val name = user?.name ?: "Unknown"`)

To indicate a reference type is nullable, the type is written as `T?`

So in the following example, the compiler will give an error when we try to access p.name
as p might be null.

    # here p is a nullable reference to a Person
    fun main(p : Person?)     
        print p.name      # this will give an error as 

The compiler can infer that the value of p is not null, based on control flow. It explicitly
looks for expressions with `= null` and `!= null`. So the following code will be OK, as the
compiler can prove that p is not null at the point where we try to access p.name

    # here p is a nullable reference to a Person
    fun main(p : Person?)     
        if p != null
            println p.name
        else
            println "nobody there"

The compiler is able to infer null safety through early returns. But not through more compelx logic.

So the following code is OK

    fun main(p : Person?)     
        if p == null
            return
        print p.name

But this is not

    fun main(p : Person?)
        val isValid = p != null          # defines isValid is a boolean
        if isValid
            print p.name                 # unfortunately the compiler can't infer that p is not null here

### Types

The fpl language has the following types:

* Unit       - represents no value
* Null       - represents a null value
* Bool
* Char
* Int
* Real       (Not yet supported on the hardware though)
* String
* Array\<T>   - an immutable array of elements of type T
* MutableArray\<T> - a mutable array of elements of type T
* Enum - a type with a fixed set of values
* Class - Similar to a struct in C. But with methods and constructors.

I might add a `Short` and `Long` type later - if I have a need for them.

### Control flow

FPL supports the usual control flow constructs

if/else

    fun main(a:Int) -> String
        if a = 0 
            return "zero"   
        else if a = 1
            return "one"
        else if a = 2
            return "two"
        else    
            return "lots"

while loops

    fun main(a:Int) -> Int
        var x = 0
        var total = 0
            while x < a
                total = total + x
                x = x + 1
        return total

repeat until

    fun main(a:Int) -> Int
        var x = 0
        var total = 0
        repeat
            total = total + x
            x = x + 1
        until x = a
        return total

for loops. I'm still deciding on the exact syntax for this. Currently for loops only
support iterating over a range. The upper limit can be inclusive `1 to 10` or 
exclusive `1 to <10`.

I will probably add syntax to iterate over arrays later.

    fun main(a:Int)
        var total = 0
        for i in 1 to <a
            println i

### Classes

Classes are defined with the 'class' keyword. They behave much like structs in C - 
fields in memory are in the same order as they are defined in the class.

Classes have a constructor. Memory is allocated automatically when the constructor
is called, but must 





class LinkedList (val value:Int, val next:LinkedList?)

fun total(list:LinkedList?)->Int
var ret = 0
var current = list
while current!=null
ret += current.value
current = current.next
return ret

fun main()
val list = new LinkedList(1, new LinkedList(2, new LinkedList(3, null)))
println(total(list))
More documentation to come later...
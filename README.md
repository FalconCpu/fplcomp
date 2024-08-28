This is my attempt at writing a compiler to target my F32 microprocessor system.

My source language, fpl, is basically C semantics with Kotlin like syntax.

 # Overall structure

## Frontend

The `Lexer` class reads an input file, and converts it into a stream of `Token` objects.

The `Parser` class reads the stream of `Token` objects, and builds an Abstract Syntax Tree, of
`AST` objects. Multiple input files can be parsed into a single AST.

Subclasses of `Ast` represent the different kinds of AST nodes. The class hierarchy is as follows:

```
Ast                        abstract - any AST node
    AstStmt                abstract - any statement
        AstBlock           abstract - a statement which may contain other statements, and a symbolTable
            AstTop         Single instance, represents the top level of the AST
            AstFunction    
            AstClass       
            AstWhile
            AstIf
            [...]
        AstDeclaration
        AstReturn
        AstPrint
        [...]
    AstExpr                 abstract - any expression
        AstIntLiteral
        AstIdentifier       Represents any identifier(variables, function names, class names, etc)
        AstBinop            Represents an arithmetic or logical binary operator
        AstEquals           Represents an == or != operator
        AstCompare          Represents a <, >, <=, or >= operator
        AstFunctionCall     Represents a function call or constructor
        AstAnd              Represents a short circuit and operator
        [...]
    AstType                 abstract - any type expression
        AstTypeId
        AstTypeArray
        [...]    
```

Each `Ast` object has a `dump` method, which prints the AST node to a StringBuilder to allow for debugging.

The `AstBlock` nodes also serve as a symbol table, and are responsible for keeping track of the names of
variables, functions, and classes defined within them. Methods `lookup` and `lookupNoHier` can be used
to search a `Symbol` associated with a given name.

After the AST is built the `identifyFunctions` method is recursively through the tree, starting from the
`AstTop` node. This method begins populating the symbol table with tha names of functions and classes.
It is responsible for identifying the names and types of function parameters and class members, but does
not concern itself with the internals of functions or constructors. This allows for forward declarations
of functions and classes.

Next the `typeCheck` method is called recursively through the tree. This method takes each node in the 
tree, together with the context of the node, and produces an instance of a `Tc` object. The `Tc` class
tree mirrors the `Ast` tree, but with extra fields to hold type information, bindings of symbols, etc.

If errors are detected at any point during the type checking a diagnostic message is reported and a `
TcError` node is produced for the node in question. All `Tc` nodes which have children will accept a
`TcError` as a child - this helps avoid to avoid an avalanche of errors.

To cater for path dependant typing the `PathContext` class is used. An instance of this class
represents the state of knowledge about Symbols at a particular point in the AST. During type
checking a new `PathContext` instance is created whenever we discover something about a Symbol.
Comparison operations can produce different `PathContext` instances depending on the truth value of
their result.

Logic in control flow classes such as `AstIf` `AstWhile` etc are responsible for passing the correct `PathContext`
instance to different the children of the node, and to call the function `mergePathContext` on the
output context of the different paths though the instruction. 

If no errors are detected during type checking, we then move onto IR code generation. The `genCode` 
method is called recursively through the tree. This method takes each node in the tree, 
and transforms it into a list of `Instr` objects which are added to the current function.
There are several different flavours of `genCode` to describe the purpose of the node. 
`genCodeRvalue` is used for nodes which are expected to return a value,
`genCodeLvalue` is used for nodes which are expected store a value into a location,
`genCodeBool` is used for nodes which represent branches in the control flow. 

## Backend

The IR form consists of a list of `Function` objects.

Each `Function` object represents a function or constructor in the source code.  
The `Function` object contains a list of `Instr`. 
Each `Instr` object represents a single step of the function in a Three Address Code form.

```
   Instr           abstract base class
       InstrMov    copy a value from one location to another
       InstrAlu    perform an arithmetic or logical operation
       InstrBranch perform a branch operation
       [...]
```

The `Function` class has a set of helper methods to generate the `Instr` objects of various flavours.

The `Reg` class represents an element of storage in the backend. This could be a `MachineReg` representing
a physical CPU register, a `UserReg` representing a variable in the function's scope, or a `TempReg`
representing the output of an alu or other `Instr`.

At present the `TempReg` instances are kept in a Single Static Assignment (SSA) form. This means that
each `TempReg` instance is assigned to exactly once. This property is by construction of the IR form,
there is currently no logic to enforce this property. So take care in any optimisations that you
do not break this property.

Currently `UserReg` and `MachineReg` instances are not kept in SSA form. Bear this in mind when writing any
optimisation passes.

Currently, the backend of the compiler is very much work in progress.

## Interpreter

The `Interpreter` class is responsible for directly executing the IR code. It is a simple interpreter which
simply executes the instructions one at a time. It is not especially efficient, but it is useful for
debugging and testing. The plan is to use this as a verification tool for the backend of the compiler.

Inside the interpreter values are represented as `Value` objects - type/value pairs. Although on the actual
CPU registers are not typed - the `Value` class within the interpreter is somewhat typed. This gives another
level of checking for bugs in the frontend of the compiler.

Functions in the standard library are executed by native code in the interpreter.
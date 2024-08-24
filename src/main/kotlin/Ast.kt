package falcon

// Base class for all AST nodes
//
// Ast
//    AstExpression
//        AstIdentifier
//        AstMember
//        ...
//    AstStatement
//        AstDeclaration
//        ...
//        AstBlockStatement
//            AstWhile
//            ...


sealed class Ast (val location: Location)
{
    abstract fun dump(sb: StringBuilder, indent: Int)

    abstract fun dumpWithType(sb: StringBuilder, indent: Int)

}

package frontend

class Token (
    val location: Location,
    val kind: TokenKind,
    val text: String
) {
    override fun toString() = text
}

enum class TokenKind (val text: String, val lineContinues : Boolean) {
    // Single character tokens
    EOF      ("<end of file>",false),
    EOL      ("<end of line>", true),
    INDENT   ("<indent>", true),
    DEDENT   ("<dedent>", true),
    INTLIT   ("<integer literal>", false),
    STRINGLIT("<string literal>", false),
    CHARLIT  ("<character literal>", false),
    REALLIT  ("<real literal>", false),
    ID       ("<identifier>", false),
    PLUS     ("+", true),
    MINUS    ("-", true),
    STAR     ("*", true),
    SLASH    ("/", true),
    PERCENT  ("%", true),
    CARET    ("^", true),
    AMPERSAND("&", true),
    BAR      ("|", true),
    EQ       ("=", true),
    NEQ      ("!=", true),
    LT       ("<", true),
    GT       (">", false),    // cannot continue a line with > as it could be close of a generic
    LTE      ("<=", true),
    GTE      (">=", true),
    IS       ("is", true),
    ISNOT    ("isnot", true),
    IN       ("in", true),
    TO       ("to", true),
    AND      ("and", true),
    NOT      ("not", true),
    OR       ("or", true),
    QMARK    ("?", false),
    ELVIS    ("?:", true),
    ARROW    ("->", true),
    BANGBANG ("!!", false),
    COLON    (":", true),
    SHL      ("shl", true),
    SHR      ("shr", true),
    USHR     ("ushr", true),
    TILDE    ("~", true),
    SEMICOLON(";", false),
    COMMA    (",", true),
    DOT      (".", true),
    OPENB    ("(", true),
    CLOSEB   (")", false),
    OPENCL   ("{", true),
    CLOSECL  ("}", false),
    OPENSQ   ("[", true),
    CLOSESQ  ("]", false),
    MUTABLEARRAY    ("MutableArray", false),
    MUTABLEARRAYOF  ("mutableArrayOf", false),
    ARRAY    ("Array", false),
    ARRAYOF  ("arrayOf", false),
    POINTER  ("Pointer", false),
    LOCAL    ("local", false),
    VAR      ("var", false),
    VAL      ("val", false),
    CONST    ("const", false),
    IF       ("if", false),
    THEN     ("then", true),
    ELSE     ("else", false),
    WHILE    ("while", false),
    END      ("end", false),
    FUN      ("fun", false),
    REPEAT   ("repeat", false),
    UNTIL    ("until", false),
    RETURN   ("return", false),
    CLASS    ("class", false),
    OVERRIDE ("override", false),
    ABSTRACT ("abstract", false),
    EXTERNAL ("@external", false),
    OPEN     ("open", false),
    ENUM     ("enum", false),
    FOR      ("for", false),
    PRINT    ("print", false),
    PRINTLN  ("println", false),
    PRINTHEX ("printhex", false),
    PRINTHEXLN ("printhexln", false),
    ERROR    ("<ERROR>", false);

    override fun toString() = text

    companion object {
        val map = entries.associateBy { it.text }
    }
}
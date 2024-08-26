package frontend

class Location (
    private val fileName: String,
    private val lineNumber: Number,
    private val columnNumber: Number
) {
    override fun toString() = "$fileName $lineNumber.$columnNumber"
}

val nullLocation = Location("null", 0, 0)
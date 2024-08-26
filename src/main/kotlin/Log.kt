import frontend.Location

object Log  {
    private val allMessages = mutableListOf<String>()
    private var numErrors = 0
    private var numWarnings = 0

    fun error(message:String) {
        allMessages.add(message)
        numErrors++
    }

    fun error(location: Location, message: String) {
        allMessages.add("$location:- $message")
        numErrors++
    }

    fun warning(message: String) {
        allMessages.add(message)
        numWarnings++
    }


    fun warning(location: Location, message: String) {
        allMessages.add("$location:- $message")
        numWarnings++
    }

    fun anyError() = numErrors!=0

    fun dump() = allMessages.joinToString(separator = "\n")

    fun initialize() {
        allMessages.clear()
        numErrors = 0
        numWarnings = 0
    }
}
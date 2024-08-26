package frontend

class ParseError(message:String) : Exception(message){

    constructor(location: Location, message: String) : this("$location:- $message")
}
package com.epam.drill.hpack

class EOFException : IOException {

    constructor() : super() {}


    constructor(s: String?) : super(s) {}

    companion object {
        private const val serialVersionUID = 6433858223774886977L
    }
}
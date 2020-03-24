package com.epam.drill.hpack

open
class IOException : Exception {

    constructor() : super() {}

    constructor(message: String?) : super(message) {}
    constructor(message: String?, cause: Throwable?) : super(message, cause) {}


    constructor(cause: Throwable?) : super(cause) {}

    companion object {
        const val serialVersionUID = 7818375828146090155L
    }
}
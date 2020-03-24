
package com.epam.drill.hpack


interface Flushable {

    @Throws(IOException::class)
    fun flush()
}
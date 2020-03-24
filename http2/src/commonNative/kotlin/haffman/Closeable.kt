package com.epam.drill.hpack


interface Closeable : AutoCloseable {
    override fun close()
}
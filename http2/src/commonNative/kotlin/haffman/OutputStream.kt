
package com.epam.drill.hpack


abstract class OutputStream : Closeable, Flushable {

    @Throws(IOException::class)
    abstract fun write(b: Int)

    @Throws(IOException::class)
    fun write(b: ByteArray) {
        write(b, 0, b.size)
    }

    @Throws(IOException::class)
    open fun write(b: ByteArray, off: Int, len: Int) {
        // len == 0 condition implicitly handled by loop bounds
        for (i in 0 until len) {
            write(b[off + i].toInt())
        }
    }

    @Throws(IOException::class)
    override fun flush() {
    }


    @Throws(IOException::class)
    override fun close() {
    }

    companion object {

        fun nullOutputStream(): OutputStream {
            return object : OutputStream() {
                private var closed = false

                @Throws(IOException::class)
                private fun ensureOpen() {
                    if (closed) {
                        throw IOException("Stream closed")
                    }
                }

                @Throws(IOException::class)
                override fun write(b: Int) {
                    ensureOpen()
                }

                @Throws(IOException::class)
                override fun write(b: ByteArray, off: Int, len: Int) {
                    ensureOpen()
                }

                override fun close() {
                    closed = true
                }
            }
        }
    }
}
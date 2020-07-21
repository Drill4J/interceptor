package com.epam.drill.interceptor

import com.epam.drill.hook.gen.*
import com.epam.drill.hook.io.*
import com.epam.drill.hook.io.tcp.*
import kotlinx.atomicfu.*
import kotlinx.cinterop.*
import kotlinx.collections.immutable.*
import kotlin.native.concurrent.*


const val HTTP2_IDENTIFICATOR = "PRI"

actual fun configureHttpInterceptor2() {
    configureTcpHooks()
    addInterceptor(Http2Interceptor().freeze())
}


@SharedImmutable
private val _http2 = atomic(persistentHashMapOf<DRILL_SOCKET, StreamPair>())
private val http2
    get() = _http2.value

class Http2Interceptor : Interceptor {
    override fun MemScope.interceptRead(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int) {
        val readBytes = bytes.readBytes(size)
        http2[fd]?.apply {
            val stream = readStream.copy(frm = readStream.frm.copy())
            if (stream.isOk()) {
                stream.readHeaders(readBytes)?.apply {
                    readHeaders.value(this)
                    readCallback.value(byteArrayOf())
                }

            } else http2.remove(fd)
            http2.put(fd, copy(readStream = stream))
        }
    }

    override fun MemScope.interceptWrite(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int): TcpFinalData {
        val readBytes = bytes.readBytes(size)
        http2[fd]?.apply {
            val stream = writeStream.copy(
                    frm = writeStream.frm.copy(), headers = injectedHeaders.value()
            )
            if (stream.isOk()) {
                val readHeaders = stream.writeHeaders(readBytes)
                if (readHeaders != null) {
                    http2.put(fd, copy(writeStream = stream))

                    return TcpFinalData(
                            readHeaders.toCValues().getPointer(this@interceptWrite),
                            readHeaders.size,
                            readHeaders.size - readBytes.size
                    )
                }
            } else {
                http2.remove(fd)
            }
            http2.put(fd, copy(writeStream = stream))
        }

        return TcpFinalData(bytes, size)
    }

    override fun close(fd: DRILL_SOCKET) {
        http2.remove(fd)
    }

    override fun isSuitableByteStream(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>): Boolean {
        val isHttp2Marker = bytes.readBytes(8).decodeToString().startsWith(HTTP2_IDENTIFICATOR)
        if (isHttp2Marker) {
            http2.put(fd, StreamPair(ReadStream(), WriteStream()))
        }
        return isHttp2Marker || http2.contains(fd)
    }

}

data class StreamPair(val readStream: ReadStream, val writeStream: WriteStream)
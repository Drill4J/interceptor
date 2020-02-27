package com.epam.drill.interceptor

import com.epam.drill.hook.gen.DRILL_SOCKET
import com.epam.drill.hook.io.TcpFinalData
import com.epam.drill.hook.io.configureTcpHooks
import com.epam.drill.hook.io.tcp.Interceptor
import com.epam.drill.hook.io.tcp.interceptor
import kotlinx.cinterop.*
import mu.KotlinLogging
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze


fun configureHttpInterceptor() {
    configureTcpHooks()
    interceptor.value = HttpInterceptor().freeze()
}


const val HTTP_DETECTOR_BYTES_COUNT = 8

const val HTTP_RESPONSE_MARKER = "HTTP"

const val FIRST_INDEX = 0

@SharedImmutable
val HTTP_VERBS =
    setOf("OPTIONS", "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "TRACE", "CONNECT") + HTTP_RESPONSE_MARKER

@SharedImmutable
val logger = KotlinLogging.logger("http")

@SharedImmutable
val CR_LF = "\r\n"

@SharedImmutable
val CR_LF_BYTES = CR_LF.encodeToByteArray()

@SharedImmutable
val HEADERS_DELIMITER = CR_LF_BYTES + CR_LF_BYTES

@SharedImmutable
val headersForInject = AtomicReference({ emptyMap<String, String>() }.freeze()).freeze()

@SharedImmutable
val readHttpCallback = AtomicReference({ _: ByteArray -> Unit }.freeze()).freeze()

@SharedImmutable
val writeHttpCallback = AtomicReference({ _: ByteArray -> Unit }.freeze()).freeze()

@ThreadLocal
private var reader = mutableMapOf<DRILL_SOCKET, ByteArray?>()


class HttpInterceptor : Interceptor {
    override fun MemScope.interceptRead(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int) {
        try {
            bytes.readBytes(HTTP_DETECTOR_BYTES_COUNT).decodeToString().let { prefix ->
                val readBytesClb = { bytes.readBytes(size.convert()) }
                when {
                    HTTP_VERBS.any { prefix.startsWith(it) } -> {
                        readBytesClb().let { readBytes -> processHttpRequest(readBytes, fd) { readBytes } }
                    }
                    reader[fd] != null -> {
                        readBytesClb().let { readBytes ->
                            processHttpRequest(readBytes, fd) {
                                reader.remove(fd)?.plus(readBytes)
                            }
                        }
                    }
                    else -> {
                    }
                }
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    override fun MemScope.interceptWrite(fd: DRILL_SOCKET, bytes: CPointer<ByteVarOf<Byte>>, size: Int): TcpFinalData {
        try {
            val readBytes = bytes.readBytes(size.convert())
            val index = readBytes.indexOf(CR_LF_BYTES)
            if (index > 0) {
                val httpWriteHeaders = headersForInject.value()
                if (isNotContainsDrillHeaders(readBytes, httpWriteHeaders)) {
                    val firstLineOfResponse = readBytes.copyOfRange(FIRST_INDEX, index)
                    val injectedHeader = prepareHeaders(httpWriteHeaders)
                    val responseTail = readBytes.copyOfRange(index, size.convert())
                    val modified = firstLineOfResponse + injectedHeader + responseTail
                    logger.debug { "App write http by '$fd' fd: \n\t${( readBytes.copyOfRange(FIRST_INDEX, readBytes.indexOf(HEADERS_DELIMITER))).decodeToString().replace("\r\n", "\r\n\t")}" }
                    writeHttpCallback.value(modified)
                    return TcpFinalData(
                        modified.toCValues().getPointer(this),
                        modified.size,
                        injectedHeader.size
                    )
                }
            }
        } catch (ex: Exception) {
            println(ex.message)
        }
        return TcpFinalData(bytes, size)
    }

    override fun isSuitableByteStream(bytes: CPointer<ByteVarOf<Byte>>): Boolean {
        return HTTP_VERBS.any { bytes.readBytes(HTTP_DETECTOR_BYTES_COUNT).decodeToString().startsWith(it) }
    }

}

private fun processHttpRequest(readBytes: ByteArray, fd: DRILL_SOCKET, dataCallback: (() -> ByteArray?)) =
    if (notContainsFullHeadersPart(readBytes)) {
        reader[fd] = reader[fd] ?: byteArrayOf() + readBytes
    } else {
        dataCallback()?.let {
            logger.debug { "App read http by '$fd' fd: \n\t${it.decodeToString().replace("\r\n", "\r\n\t")}" }
            readHttpCallback.value(it)
        }

    }


private fun notContainsFullHeadersPart(readBytes: ByteArray) = readBytes.indexOf(HEADERS_DELIMITER) == -1


private fun prepareHeaders(httpWriteHeaders: Map<String, String>) =
    CR_LF_BYTES + httpWriteHeaders.map { (k, v) -> "$k: $v" }.joinToString(CR_LF).encodeToByteArray()

private fun isNotContainsDrillHeaders(readBytes: ByteArray, httpWriteHeaders: Map<String, String>) =
    httpWriteHeaders.isNotEmpty() && readBytes.indexOf(httpWriteHeaders.entries.first().key.encodeToByteArray()) == -1
package com.epam.drill.interceptor

import com.epam.drill.hpack.*
import kotlin.experimental.and

@SharedImmutable
val CR_LF = "\r\n"

@SharedImmutable
val CR_LF_BYTES = CR_LF.encodeToByteArray()

@SharedImmutable
val HEADERS_DELIMITER = CR_LF_BYTES + CR_LF_BYTES


inline class Opcode(val id: Int) {

    companion object {
        val Data = Opcode(0x0)
        val Headers = Opcode(0x1)
        val Priority = Opcode(0x2)
        val RstStream = Opcode(0x3)
        val Settings = Opcode(0x4)
        val PushPromise = Opcode(0x5)
        val Ping = Opcode(0x6)
        val GoAway = Opcode(0x7)
        val WindowUpdate = Opcode(0x8)
        val Continuation = Opcode(0x9)
    }
}

fun Int.extract(offset: Int, count: Int): Int = (this ushr offset) and count.mask()
fun Int.extract(offset: Int): Boolean = ((this ushr offset) and 1) != 0
fun Int.mask(): Int = (1 shl this) - 1
fun ByteArray.u8(o: Int): Int = this[o].toInt() and 0xFF
fun ByteArray.read16BE(o: Int): Int = (u8(o + 1) shl 0) or (u8(o + 0) shl 8)
fun ByteArray.read24BE(o: Int): Int = (u8(o + 2) shl 0) or (u8(o + 1) shl 8) or (u8(o + 0) shl 16)
fun ByteArray.read32BE(o: Int): Int =
    (u8(o + 3) shl 0) or (u8(o + 2) shl 8) or (u8(o + 1) shl 16) or (u8(o + 0) shl 24)

fun Int.write32BE(): ByteArray = run {
    byteArrayOf(
        (this ushr 24).toByte() and 0xFF.toByte(),
        (this ushr 16).toByte() and 0xFF.toByte(),
        (this ushr 8).toByte() and 0xFF.toByte(),
        (this and 0xFF).toByte()
    )
}

fun Int.write24BE(): ByteArray = run {
    byteArrayOf(
        (this ushr 16).toByte() and 0xFF.toByte(), (this ushr 8).toByte() and 0xFF.toByte(), (this and 0xFF).toByte()
    )
}


data class Frame(
    var frameStartIndex: Int = 0,
    var frameEndIndex: Int = 0,
    var length: Int = 0,
    var type: Int = 0,
    var flags: Int = 0,
    var streamIdentifier: Int = 0,
    var payload: ByteArray = byteArrayOf()
) {
    fun toByteArray(): ByteArray {
        var q = byteArrayOf()
        q += length.write24BE()
        q += byteArrayOf(type.toByte())
        q += byteArrayOf(flags.toByte())
        q += streamIdentifier.write32BE()
        q += payload
        return q
    }


}


package com.epam.drill.hpack

interface HeaderListener {

    fun addHeader(name: ByteArray?, value: ByteArray?, sensitive: Boolean)
}

package com.epam.drill.hpack

object Huffman {

    val DECODER = HuffmanDecoder(HpackUtil.HUFFMAN_CODES, HpackUtil.HUFFMAN_CODE_LENGTHS)


    val ENCODER = HuffmanEncoder(HpackUtil.HUFFMAN_CODES, HpackUtil.HUFFMAN_CODE_LENGTHS)
}
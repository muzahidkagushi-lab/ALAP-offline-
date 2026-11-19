package com.alap.mesh.util

import java.security.MessageDigest

object BanglaFormatters {
    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBanglaDigits(number: Int): String = toBanglaDigits(number.toString())

    fun toBanglaDigits(input: String): String {
        return input.map { ch ->
            if (ch in '0'..'9') banglaDigits[ch - '0'] else ch
        }.joinToString("")
    }

    fun nodeIdToHash32(nodeId: String): Int {
        val digest = MessageDigest.getInstance("SHA-256").digest(nodeId.toByteArray(Charsets.UTF_8))
        return ((digest[0].toInt() and 0xFF) shl 24) or
            ((digest[1].toInt() and 0xFF) shl 16) or
            ((digest[2].toInt() and 0xFF) shl 8) or
            (digest[3].toInt() and 0xFF)
    }

    fun nodeIdToShortId(nodeId: String): Short {
        val h = nodeIdToHash32(nodeId)
        val s = (h and 0xFFFF).toShort()
        return if (s.toInt() == 0) 1 else s
    }

    fun shortIdHex(shortId: Short): String = "%04X".format(shortId.toInt() and 0xFFFF)

    fun hash32Hex(hash: Int): String = "%08X".format(hash)
}

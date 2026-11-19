package com.alap.mesh

import com.alap.mesh.util.BanglaFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun banglaDigitsAndShortIdWorkCorrectly() {
        assertEquals("১২৩৪", BanglaFormatters.toBanglaDigits(1234))
        assertEquals("০", BanglaFormatters.toBanglaDigits(0))
        val shortA = BanglaFormatters.nodeIdToShortId("ALAP-AAAA-111")
        val shortB = BanglaFormatters.nodeIdToShortId("ALAP-BBBB-222")
        assertNotEquals(0.toShort(), shortA)
        assertNotEquals(shortA, shortB)
    }
}

package com.alap.mesh

import com.alap.mesh.mesh.protocol.AckPayload
import com.alap.mesh.mesh.protocol.BinaryMeshPacket
import com.alap.mesh.mesh.protocol.HandshakePayload
import com.alap.mesh.mesh.protocol.PacketType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BinaryMeshPacketTest {

    @Test
    fun `encode decode roundtrip preserves bangla newlines pipes emojis and hop path`() {
        val originalText = "হ্যালো বাংলাদেশ!\nলাইন ২ | পাইপ টেস্ট 🙂🔥"
        val packet = BinaryMeshPacket(
            type = PacketType.DATA,
            flags = BinaryMeshPacket.FLAG_SIGNED.toByte(),
            messageId = 0x1122334455667788L,
            ttl = 4,
            hopCount = 2,
            senderHash = 0x1234ABCD,
            recipientHash = 0x5678EF01,
            timestampSeconds = 1_750_000_000,
            hopShortIds = listOf(0x0102.toShort(), 0x7F01.toShort()),
            payload = originalText.toByteArray(Charsets.UTF_8)
        )

        val decoded = BinaryMeshPacket.decode(packet.encode())
        assertNotNull(decoded)
        assertEquals(packet, decoded)
        assertEquals(originalText, String(decoded!!.payload, Charsets.UTF_8))
        assertTrue(decoded.isSigned)
        assertFalse(decoded.isEncrypted)
    }

    @Test
    fun `forwardCopy decrements ttl appends relay shortId and stops when ttl is 1`() {
        val initial = BinaryMeshPacket(
            type = PacketType.PUBLIC,
            ttl = 2,
            hopCount = 0,
            senderHash = 100,
            recipientHash = BinaryMeshPacket.BROADCAST_HASH,
            payload = "test".toByteArray()
        )

        val hop1 = initial.forwardCopy(0x1A2B.toShort())
        assertNotNull(hop1)
        assertEquals(1, hop1!!.ttl)
        assertEquals(1, hop1.hopCount)
        assertEquals(listOf(0x1A2B.toShort()), hop1.hopShortIds)

        val hop2 = hop1.forwardCopy(0x3C4D.toShort())
        assertNull(hop2)
    }

    @Test
    fun `clock skew check accepts within 10 minutes and rejects outside`() {
        val nowSec = 1_750_000_000
        val okPast = BinaryMeshPacket(
            type = PacketType.DATA,
            senderHash = 1,
            recipientHash = 2,
            timestampSeconds = nowSec - 599,
            payload = byteArrayOf(1)
        )
        val okFuture = okPast.copy(timestampSeconds = nowSec + 599)
        val badPast = okPast.copy(timestampSeconds = nowSec - 900)
        val badFuture = okPast.copy(timestampSeconds = nowSec + 900)

        assertTrue(okPast.isTimestampWithinSkew(nowSec))
        assertTrue(okFuture.isTimestampWithinSkew(nowSec))
        assertFalse(badPast.isTimestampWithinSkew(nowSec))
        assertFalse(badFuture.isTimestampWithinSkew(nowSec))
    }

    @Test
    fun `ack and handshake payloads roundtrip cleanly`() {
        val ack = AckPayload(
            ackedMessageId = 987654321L,
            reachedHopCount = 3,
            routeShortIds = listOf(0x0011.toShort(), 0x0022.toShort(), 0x0033.toShort())
        )
        val decodedAck = AckPayload.decode(ack.encode())
        assertEquals(ack, decodedAck)

        val hsVisible = HandshakePayload(
            nodeId = "ALAP-A1B2-C3D",
            shortId = 0x4567.toShort(),
            displayName = "মুজাহিদ",
            stealth = false,
            publicKeyBytes = byteArrayOf(1, 2, 3, 4)
        )
        val decodedVisible = HandshakePayload.decode(hsVisible.encode())
        assertNotNull(decodedVisible)
        assertEquals("মুজাহিদ", decodedVisible!!.displayName)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), decodedVisible.publicKeyBytes)

        val hsStealth = hsVisible.copy(stealth = true)
        val decodedStealth = HandshakePayload.decode(hsStealth.encode())
        assertNotNull(decodedStealth)
        assertEquals("Stealth", decodedStealth!!.displayName)
        assertTrue(decodedStealth.stealth)
    }

    @Test
    fun `decode rejects malformed or truncated frames`() {
        assertNull(BinaryMeshPacket.decode(ByteArray(0)))
        assertNull(BinaryMeshPacket.decode(ByteArray(10)))
        val badMagic = BinaryMeshPacket(
            type = PacketType.DATA,
            senderHash = 1,
            recipientHash = 2,
            payload = "abc".toByteArray()
        ).encode().also { it[0] = 0x00 }
        assertNull(BinaryMeshPacket.decode(badMagic))
    }
}

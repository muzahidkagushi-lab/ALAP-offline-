package com.alap.mesh

import com.alap.mesh.mesh.protocol.FragmentCodec
import com.alap.mesh.mesh.protocol.FragmentReassembler
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FragmentCodecTest {

    @Test
    fun `multi chunk roundtrip reassembles out of order and ignores duplicates`() {
        var nowMs = 1_000L
        val reassembler = FragmentReassembler(clock = { nowMs })
        val payload = ("আলাপ মেশ ফ্রেম টেস্ট — ").repeat(40).toByteArray(Charsets.UTF_8)

        val chunks = FragmentCodec.fragment(messageId = 42L, frameBytes = payload, maxChunkData = 100)
        assertEquals(true, chunks.size >= 4)

        // Deliver out of order + duplicate chunk 0
        assertNull(reassembler.onFragmentReceived("AA:BB:CC:DD", chunks[2]))
        assertNull(reassembler.onFragmentReceived("AA:BB:CC:DD", chunks[0]))
        assertNull(reassembler.onFragmentReceived("AA:BB:CC:DD", chunks[0])) // duplicate
        assertNull(reassembler.onFragmentReceived("AA:BB:CC:DD", chunks[chunks.lastIndex]))

        var result: ByteArray? = null
        for (i in 1 until chunks.lastIndex) {
            if (i == 2) continue
            val candidate = reassembler.onFragmentReceived("AA:BB:CC:DD", chunks[i])
            if (candidate != null) result = candidate
        }

        assertNotNull(result)
        assertArrayEquals(payload, result)
        assertEquals(0, reassembler.pendingCount())
    }

    @Test
    fun `empty frame roundtrips cleanly`() {
        val reassembler = FragmentReassembler()
        val chunks = FragmentCodec.fragment(messageId = 99L, frameBytes = ByteArray(0), maxChunkData = 100)
        assertEquals(1, chunks.size)
        val result = reassembler.onFragmentReceived("peer1", chunks[0])
        assertNotNull(result)
        assertEquals(0, result!!.size)
    }

    @Test
    fun `incomplete assembly is evicted after timeout`() {
        var nowMs = 10_000L
        val reassembler = FragmentReassembler(timeoutMs = 15_000L, clock = { nowMs })
        val payload = ByteArray(500) { (it % 127).toByte() }
        val chunks = FragmentCodec.fragment(messageId = 7L, frameBytes = payload, maxChunkData = 120)

        assertNull(reassembler.onFragmentReceived("peer1", chunks[0]))
        assertEquals(1, reassembler.pendingCount())

        nowMs += 15_001L
        // Trigger eviction via next check
        assertEquals(0, reassembler.pendingCount())
        // Remaining chunk alone will not reconstruct missing chunk 0
        assertNull(reassembler.onFragmentReceived("peer1", chunks[1]))
    }

    @Test
    fun `rejects oversized frame and cleans up per peer`() {
        val reassembler = FragmentReassembler(maxFrameBytes = 250)
        val bigPayload = ByteArray(400) { 0x55 }
        val chunks = FragmentCodec.fragment(messageId = 88L, frameBytes = bigPayload, maxChunkData = 120)

        assertNull(reassembler.onFragmentReceived("peerA", chunks[0]))
        assertNull(reassembler.onFragmentReceived("peerA", chunks[1]))
        // 3rd chunk crosses 250 byte cap -> rejected and removed
        assertNull(reassembler.onFragmentReceived("peerA", chunks[2]))
        assertEquals(0, reassembler.pendingCount())

        // clearPeer removes pending assemblies for specific peer only
        assertNull(reassembler.onFragmentReceived("peerX", chunks[0]))
        assertNull(reassembler.onFragmentReceived("peerY", chunks[0]))
        assertEquals(2, reassembler.pendingCount())
        reassembler.clearPeer("peerX")
        assertEquals(1, reassembler.pendingCount())
    }
}

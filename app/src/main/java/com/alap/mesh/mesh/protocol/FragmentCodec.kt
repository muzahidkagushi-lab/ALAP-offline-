package com.alap.mesh.mesh.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

object FragmentCodec {
    const val FRAGMENT_MAGIC: Byte = 0xF1.toByte()
    const val FRAGMENT_HEADER_BYTES = 15 // magic(1) + msgId(8) + seq(2) + total(2) + chunkLen(2)
    const val DEFAULT_MAX_CHUNK_DATA = 160

    fun fragment(messageId: Long, frameBytes: ByteArray, maxChunkData: Int = DEFAULT_MAX_CHUNK_DATA): List<ByteArray> {
        val chunkPayloadLimit = maxChunkData.coerceAtLeast(16)
        if (frameBytes.isEmpty()) {
            val buf = ByteBuffer.allocate(FRAGMENT_HEADER_BYTES).order(ByteOrder.BIG_ENDIAN)
            buf.put(FRAGMENT_MAGIC)
            buf.putLong(messageId)
            buf.putShort(0)
            buf.putShort(1)
            buf.putShort(0)
            return listOf(buf.array())
        }
        val totalChunks = ((frameBytes.size + chunkPayloadLimit - 1) / chunkPayloadLimit)
        val result = ArrayList<ByteArray>(totalChunks)
        var offset = 0
        var seq = 0
        while (offset < frameBytes.size) {
            val len = minOf(chunkPayloadLimit, frameBytes.size - offset)
            val buf = ByteBuffer.allocate(FRAGMENT_HEADER_BYTES + len).order(ByteOrder.BIG_ENDIAN)
            buf.put(FRAGMENT_MAGIC)
            buf.putLong(messageId)
            buf.putShort(seq.toShort())
            buf.putShort(totalChunks.toShort())
            buf.putShort(len.toShort())
            buf.put(frameBytes, offset, len)
            result.add(buf.array())
            offset += len
            seq++
        }
        return result
    }

    data class DecodedFragment(
        val messageId: Long,
        val seq: Int,
        val total: Int,
        val data: ByteArray
    )

    fun decodeFragment(bytes: ByteArray): DecodedFragment? {
        if (bytes.size < FRAGMENT_HEADER_BYTES) return null
        return try {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            if (buf.get() != FRAGMENT_MAGIC) return null
            val messageId = buf.getLong()
            val seq = buf.getShort().toInt() and 0xFFFF
            val total = buf.getShort().toInt() and 0xFFFF
            val chunkLen = buf.getShort().toInt() and 0xFFFF
            if (total == 0 || seq >= total || chunkLen > buf.remaining()) return null
            val data = ByteArray(chunkLen)
            buf.get(data)
            DecodedFragment(messageId, seq, total, data)
        } catch (_: Exception) {
            null
        }
    }
}

class FragmentReassembler(
    private val maxFrameBytes: Int = 16_384,
    private val timeoutMs: Long = 15_000L,
    private val maxPendingAssemblies: Int = 64,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private data class AssemblyKey(val peerKey: String, val messageId: Long)

    private class PendingAssembly(
        val total: Int,
        val createdAtMs: Long
    ) {
        val chunks = arrayOfNulls<ByteArray>(total)
        var receivedCount = 0
        var totalBytes = 0
    }

    private val pending = LinkedHashMap<AssemblyKey, PendingAssembly>()

    @Synchronized
    fun onFragmentReceived(peerKey: String, rawFragment: ByteArray): ByteArray? {
        evictExpiredLocked()
        val frag = FragmentCodec.decodeFragment(rawFragment) ?: return null
        val key = AssemblyKey(peerKey, frag.messageId)

        var assembly = pending[key]
        if (assembly == null) {
            if (frag.total > 256) return null
            if (pending.size >= maxPendingAssemblies) {
                val oldestKey = pending.keys.firstOrNull()
                if (oldestKey != null) pending.remove(oldestKey)
            }
            assembly = PendingAssembly(frag.total, clock())
            pending[key] = assembly
        } else if (assembly.total != frag.total) {
            pending.remove(key)
            return null
        }

        if (assembly.chunks[frag.seq] == null) {
            if (assembly.totalBytes + frag.data.size > maxFrameBytes) {
                pending.remove(key)
                return null
            }
            assembly.chunks[frag.seq] = frag.data
            assembly.receivedCount++
            assembly.totalBytes += frag.data.size
        }

        if (assembly.receivedCount == assembly.total) {
            pending.remove(key)
            val combined = ByteArray(assembly.totalBytes)
            var offset = 0
            for (chunk in assembly.chunks) {
                if (chunk == null) return null
                System.arraycopy(chunk, 0, combined, offset, chunk.size)
                offset += chunk.size
            }
            return combined
        }
        return null
    }

    @Synchronized
    fun pendingCount(): Int {
        evictExpiredLocked()
        return pending.size
    }

    @Synchronized
    fun clearPeer(peerKey: String) {
        val iter = pending.keys.iterator()
        while (iter.hasNext()) {
            if (iter.next().peerKey == peerKey) iter.remove()
        }
    }

    private fun evictExpiredLocked() {
        val now = clock()
        val iter = pending.entries.iterator()
        while (iter.hasNext()) {
            if (now - iter.next().value.createdAtMs > timeoutMs) {
                iter.remove()
            }
        }
    }
}

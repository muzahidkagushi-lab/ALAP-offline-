package com.alap.mesh.mesh.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom

enum class PacketType(val code: Byte) {
    DATA(1),
    ACK(2),
    HANDSHAKE(3),
    PING(4),
    SOS(5),
    PUBLIC(6);

    companion object {
        fun fromCode(code: Byte): PacketType? = entries.find { it.code == code }
    }
}

data class BinaryMeshPacket(
    val version: Byte = PROTOCOL_VERSION,
    val type: PacketType,
    val flags: Byte = 0,
    val messageId: Long = nextMessageId(),
    val ttl: Int = 4,
    val hopCount: Int = 0,
    val senderHash: Int,
    val recipientHash: Int,
    val timestampSeconds: Int = (System.currentTimeMillis() / 1000L).toInt(),
    val hopShortIds: List<Short> = emptyList(),
    val payload: ByteArray
) {
    val isEncrypted: Boolean get() = (flags.toInt() and FLAG_ENCRYPTED) != 0
    val isSigned: Boolean get() = (flags.toInt() and FLAG_SIGNED) != 0
    val isSos: Boolean get() = type == PacketType.SOS || (flags.toInt() and FLAG_SOS) != 0

    fun messageIdHex(): String = "%016X".format(messageId)

    fun forwardCopy(relayShortId: Short): BinaryMeshPacket? {
        if (ttl <= 1) return null
        val updatedPath = if (hopShortIds.size < MAX_HOP_PATH_ENTRIES) {
            hopShortIds + relayShortId
        } else {
            hopShortIds
        }
        return copy(
            ttl = ttl - 1,
            hopCount = hopCount + 1,
            hopShortIds = updatedPath
        )
    }

    fun isTimestampWithinSkew(nowSeconds: Int = (System.currentTimeMillis() / 1000L).toInt()): Boolean {
        val diff = kotlin.math.abs(nowSeconds.toLong() - timestampSeconds.toLong())
        return diff <= CLOCK_SKEW_TOLERANCE_SECONDS
    }

    fun encode(): ByteArray {
        val pathCount = hopShortIds.size.coerceAtMost(MAX_HOP_PATH_ENTRIES)
        val payloadLen = payload.size.coerceAtMost(MAX_PAYLOAD_BYTES)
        val totalSize = FIXED_HEADER_BYTES + (pathCount * 2) + 2 + payloadLen
        val buf = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN)

        buf.put(MAGIC)
        buf.put(version)
        buf.put(type.code)
        buf.put(flags)
        buf.putLong(messageId)
        buf.put(ttl.coerceIn(0, 255).toByte())
        buf.put(hopCount.coerceIn(0, 255).toByte())
        buf.putInt(senderHash)
        buf.putInt(recipientHash)
        buf.putInt(timestampSeconds)
        buf.put(pathCount.toByte())
        for (i in 0 until pathCount) {
            buf.putShort(hopShortIds[i])
        }
        buf.putShort(payloadLen.toShort())
        buf.put(payload, 0, payloadLen)
        return buf.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BinaryMeshPacket) return false
        return version == other.version &&
            type == other.type &&
            flags == other.flags &&
            messageId == other.messageId &&
            ttl == other.ttl &&
            hopCount == other.hopCount &&
            senderHash == other.senderHash &&
            recipientHash == other.recipientHash &&
            timestampSeconds == other.timestampSeconds &&
            hopShortIds == other.hopShortIds &&
            payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = messageId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + ttl
        result = 31 * result + hopCount
        result = 31 * result + payload.contentHashCode()
        return result
    }

    companion object {
        const val MAGIC: Byte = 0xA1.toByte()
        const val PROTOCOL_VERSION: Byte = 0x01
        const val BROADCAST_HASH: Int = -1
        const val FIXED_HEADER_BYTES = 27
        const val MAX_HOP_PATH_ENTRIES = 12
        const val MAX_PAYLOAD_BYTES = 16000
        const val MAX_TEXT_CHARS = 500
        const val CLOCK_SKEW_TOLERANCE_SECONDS = 600L // +/- 10 minutes

        const val FLAG_ENCRYPTED = 0x01
        const val FLAG_SIGNED = 0x02
        const val FLAG_SOS = 0x04

        private val rng = SecureRandom()

        fun nextMessageId(): Long = rng.nextLong()

        fun decode(bytes: ByteArray): BinaryMeshPacket? {
            if (bytes.size < FIXED_HEADER_BYTES + 2) return null
            return try {
                val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
                val magic = buf.get()
                if (magic != MAGIC) return null
                val version = buf.get()
                val type = PacketType.fromCode(buf.get()) ?: return null
                val flags = buf.get()
                val messageId = buf.getLong()
                val ttl = buf.get().toInt() and 0xFF
                val hopCount = buf.get().toInt() and 0xFF
                val senderHash = buf.getInt()
                val recipientHash = buf.getInt()
                val timestampSeconds = buf.getInt()
                val pathCount = buf.get().toInt() and 0xFF
                if (pathCount > MAX_HOP_PATH_ENTRIES) return null
                if (buf.remaining() < (pathCount * 2) + 2) return null

                val hops = ArrayList<Short>(pathCount)
                repeat(pathCount) {
                    hops.add(buf.getShort())
                }
                val payloadLen = buf.getShort().toInt() and 0xFFFF
                if (payloadLen > MAX_PAYLOAD_BYTES || buf.remaining() < payloadLen) return null
                val payload = ByteArray(payloadLen)
                buf.get(payload)

                BinaryMeshPacket(
                    version = version,
                    type = type,
                    flags = flags,
                    messageId = messageId,
                    ttl = ttl,
                    hopCount = hopCount,
                    senderHash = senderHash,
                    recipientHash = recipientHash,
                    timestampSeconds = timestampSeconds,
                    hopShortIds = hops,
                    payload = payload
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

data class HandshakePayload(
    val nodeId: String,
    val shortId: Short,
    val displayName: String,
    val stealth: Boolean,
    val publicKeyBytes: ByteArray = ByteArray(0)
) {
    fun encode(): ByteArray {
        val nodeBytes = nodeId.toByteArray(Charsets.UTF_8).take(48).toByteArray()
        val nameBytes = (if (stealth) "Stealth" else displayName).toByteArray(Charsets.UTF_8).take(48).toByteArray()
        val keyBytes = publicKeyBytes.take(128).toByteArray()
        val buf = ByteBuffer.allocate(1 + 2 + 1 + nodeBytes.size + 1 + nameBytes.size + 2 + keyBytes.size)
            .order(ByteOrder.BIG_ENDIAN)
        buf.put(if (stealth) 1.toByte() else 0.toByte())
        buf.putShort(shortId)
        buf.put(nodeBytes.size.toByte())
        buf.put(nodeBytes)
        buf.put(nameBytes.size.toByte())
        buf.put(nameBytes)
        buf.putShort(keyBytes.size.toShort())
        buf.put(keyBytes)
        return buf.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HandshakePayload) return false
        return nodeId == other.nodeId && shortId == other.shortId && displayName == other.displayName && stealth == other.stealth
    }

    override fun hashCode(): Int = nodeId.hashCode()

    companion object {
        fun decode(bytes: ByteArray): HandshakePayload? {
            if (bytes.size < 6) return null
            return try {
                val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
                val stealth = buf.get().toInt() == 1
                val shortId = buf.getShort()
                val nodeLen = buf.get().toInt() and 0xFF
                if (buf.remaining() < nodeLen + 1) return null
                val nodeBytes = ByteArray(nodeLen).also { buf.get(it) }
                val nameLen = buf.get().toInt() and 0xFF
                if (buf.remaining() < nameLen + 2) return null
                val nameBytes = ByteArray(nameLen).also { buf.get(it) }
                val keyLen = buf.getShort().toInt() and 0xFFFF
                if (buf.remaining() < keyLen) return null
                val keyBytes = ByteArray(keyLen).also { buf.get(it) }
                HandshakePayload(
                    nodeId = String(nodeBytes, Charsets.UTF_8),
                    shortId = shortId,
                    displayName = String(nameBytes, Charsets.UTF_8),
                    stealth = stealth,
                    publicKeyBytes = keyBytes
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

data class AckPayload(
    val ackedMessageId: Long,
    val reachedHopCount: Int,
    val routeShortIds: List<Short>
) {
    fun encode(): ByteArray {
        val count = routeShortIds.size.coerceAtMost(BinaryMeshPacket.MAX_HOP_PATH_ENTRIES)
        val buf = ByteBuffer.allocate(8 + 1 + 1 + count * 2).order(ByteOrder.BIG_ENDIAN)
        buf.putLong(ackedMessageId)
        buf.put(reachedHopCount.coerceIn(0, 255).toByte())
        buf.put(count.toByte())
        for (i in 0 until count) {
            buf.putShort(routeShortIds[i])
        }
        return buf.array()
    }

    companion object {
        fun decode(bytes: ByteArray): AckPayload? {
            if (bytes.size < 10) return null
            return try {
                val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
                val msgId = buf.getLong()
                val hopCount = buf.get().toInt() and 0xFF
                val count = buf.get().toInt() and 0xFF
                if (count > BinaryMeshPacket.MAX_HOP_PATH_ENTRIES || buf.remaining() < count * 2) return null
                val list = ArrayList<Short>(count)
                repeat(count) { list.add(buf.getShort()) }
                AckPayload(msgId, hopCount, list)
            } catch (_: Exception) {
                null
            }
        }
    }
}

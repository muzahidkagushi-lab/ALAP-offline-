package com.alap.mesh.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

object DeliveryStatus {
    const val SENDING = "SENDING"
    const val QUEUED = "QUEUED"
    const val SENT_TO_NEIGHBOR = "SENT_TO_NEIGHBOR"
    const val DELIVERED = "DELIVERED"
    const val RECEIVED = "RECEIVED"
    const val FAILED = "FAILED"

    fun toBanglaLabel(status: String, hopCount: Int = 0): String = when (status) {
        SENDING -> "পাঠানো হচ্ছে"
        QUEUED -> "কিউতে (কোনো পিয়ার নেই)"
        SENT_TO_NEIGHBOR -> "পাঠানো হয়েছে (প্রতিবেশীর কাছে)"
        DELIVERED -> if (hopCount > 0) "পৌঁছেছে ($hopCount হপ)" else "পৌঁছেছে (ACK পেয়েছি)"
        RECEIVED -> if (hopCount > 0) "প্রাপ্ত ($hopCount হপ)" else "প্রাপ্ত"
        FAILED -> "ব্যর্থ"
        else -> status
    }
}

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarShort: String,
    val type: String, // "direct", "group", "channel"
    val hopBadge: String,
    val unreadCount: Int = 0,
    val lastMessage: String,
    val lastMessageTime: String,
    val deliveryStatus: String,
    val hasFile: Boolean = false,
    val peerNodeId: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val isMe: Boolean,
    val text: String,
    val timeLabel: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val deliveryStatus: String = DeliveryStatus.SENDING,
    val hopCount: Int = 0,
    val hopMeta: String = "",
    val hopPathSerialized: String = "", // Pipe-separated path steps
    val isFile: Boolean = false,
    val fileName: String = "",
    val fileSize: String = "",
    val fileType: String = "", // "image", "video", "audio", "document"
    val isSos: Boolean = false
)

@Entity(tableName = "file_transfers")
data class FileTransferEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val fileType: String, // "video", "document", "image", "audio"
    val sizeText: String,
    val speedText: String,
    val medium: String,
    val direction: String, // "sent" or "received"
    val peerName: String,
    val progressPercent: Int = 0,
    val status: String = DeliveryStatus.QUEUED,
    val localUri: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "mesh_peers")
data class MeshPeerEntity(
    @PrimaryKey val nodeId: String,
    val shortId: Int = 0,
    val nodeHash32: Int = 0,
    val name: String,
    val avatarShort: String,
    val hopCount: Int,
    val hopLabel: String,
    val linkType: String,
    val rssiDbm: Int,
    val signalLabel: String,
    val statusText: String,
    val pathSerialized: String,
    val colorKey: String = "sky",
    val publicKeyHex: String = "",
    val isVerified: Boolean = false,
    val isConnected: Boolean = false,
    val lastSeenMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "pending_packets")
data class PendingPacketEntity(
    @PrimaryKey val messageIdHex: String,
    val recipientHash: Int,
    val packetBytes: ByteArray,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val retryCount: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PendingPacketEntity) return false
        return messageIdHex == other.messageIdHex && packetBytes.contentEquals(other.packetBytes)
    }

    override fun hashCode(): Int = messageIdHex.hashCode()
}

@Entity(tableName = "seen_packets")
data class SeenPacketEntity(
    @PrimaryKey val messageIdHex: String,
    val seenAtMillis: Long
)

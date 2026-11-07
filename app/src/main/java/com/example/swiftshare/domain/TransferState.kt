package com.example.swiftshare.domain

import java.util.UUID

enum class Direction { SEND, RECEIVE }
enum class Role { SENDER, RECEIVER }
enum class Status { COMPLETED, FAILED, CANCELED, IN_PROGRESS }

/**
 * A single entry in the transfer history. File contents are never stored.
 */
data class TransferRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val direction: Direction = Direction.SEND,
    val role: Role = Role.SENDER,
    val fileName: String = "",
    val fileSize: Long = 0,
    val status: Status = Status.IN_PROGRESS,
    val peerName: String? = null,
    val averageSpeedMbps: Double = 0.0,
    val durationSeconds: Long = 0
)

/**
 * A file chosen by the user through the system file picker.
 */
data class FileItem(
    val uri: String,
    val name: String,
    val size: Long,
    val mimeType: String? = null
)

/**
 * A peer discovered through Nearby Connections.
 */
data class DiscoveredPeer(
    val endpointId: String,
    val name: String
)

/**
 * High-level UI state exposed by the transfer manager.
 */
sealed class TransferState {
    object Idle : TransferState()
    data class Discovering(val role: Role) : TransferState()
    data class FoundPeers(val peers: List<DiscoveredPeer>, val role: Role) : TransferState()
    data class NoPeers(val role: Role) : TransferState()
    data class IncomingRequest(
        val endpointId: String,
        val peerName: String,
        val code: String? = null
    ) : TransferState()
    data class Connecting(
        val peerName: String,
        val role: Role,
        val code: String? = null
    ) : TransferState()
    data class Connected(val peerName: String, val role: Role) : TransferState()
    data class Transferring(
        val role: Role,
        val fileName: String,
        val index: Int,
        val totalFiles: Int,
        val transferredBytes: Long,
        val totalBytes: Long,
        val speedMbps: Double,
        val etaSeconds: Long?
    ) : TransferState()
    data class Completed(val fileName: String, val role: Role) : TransferState()
    data class Failed(val message: String) : TransferState()
    data class Canceled(val message: String? = null) : TransferState()
}

package com.project.chat.presentation.models

/**
 * An image staged in the composer before sending.
 *
 * Lifecycle: [PendingAttachmentStatus.PROCESSING] (compressing, shows a file icon) →
 * [PendingAttachmentStatus.READY] (thumbnail) → [PendingAttachmentStatus.UPLOADING] (on send) →
 * cleared on success or [PendingAttachmentStatus.FAILED] on error.
 *
 * @param bytes the original bytes while processing, then the compressed bytes once ready (also what gets
 * uploaded).
 */
data class PendingAttachmentUi(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    val status: PendingAttachmentStatus,
    val durationInSeconds: Int? = null,
) {
    // ByteArray needs structural equals/hashCode for correct list diffing in Compose state.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PendingAttachmentUi) return false
        return id == other.id &&
            fileName == other.fileName &&
            mimeType == other.mimeType &&
            status == other.status &&
            durationInSeconds == other.durationInSeconds &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + (durationInSeconds ?: 0)
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

enum class PendingAttachmentStatus {
    PROCESSING,
    READY,
    UPLOADING,
    FAILED,
}

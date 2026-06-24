package com.project.chat.domain.models

/**
 * Signals that the local user just lost access to a chat, with the reason so the UI can explain why the chat
 * disappeared (rather than it vanishing silently, which reads as a bug).
 */
data class ChatRemoval(
    val chatId: String,
    val reason: ChatRemovalReason,
)

enum class ChatRemovalReason {
    /** An admin removed the local user from the chat. */
    REMOVED_BY_ADMIN,

    /** The chat was deleted by someone else (its creator) — shown to the other participants. */
    CHAT_DELETED,

    /** The local user (the creator) deleted the chat themselves — shown as a success confirmation. */
    CHAT_DELETED_BY_ME,
}

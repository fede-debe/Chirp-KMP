package com.project.chat.domain.message

import com.project.chat.domain.models.ChatMessageDeliveryStatus
import com.project.core.domain.util.DataError
import com.project.core.domain.util.EmptyResult

/**
 * Repository interface defining local data operations specific to individual chat messages.
 *
 * ## Strategy / Decisions
 * Separated from the broader Chat Repository to handle granular, message-specific mutations—specifically updating delivery statuses during network failures—without bloating the general chat abstractions.
 *
 * ## How It Works
 * 1. Defines `updateMessageDeliveryStatus` which takes a target message ID and a new delivery state.
 * 2. Returns an empty `Result` containing a `DataError.Local` if the operation fails.
 */
interface MessageRepository {
    suspend fun updateMessageDeliveryStatus(
        messageId: String,
        status: ChatMessageDeliveryStatus,
    ): EmptyResult<DataError.Local>
}

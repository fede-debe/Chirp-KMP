package com.project.chat.data.mappers

import com.project.chat.data.dto.ChatAttachmentDto
import com.project.chat.data.dto.ChatMessageDto
import com.project.chat.data.dto.request.AttachmentInput
import com.project.chat.data.dto.websocket.IncomingWebSocketDto
import com.project.chat.data.dto.websocket.OutgoingWebSocketDto
import com.project.chat.database.entities.ChatMessageEntity
import com.project.chat.database.view.LastMessageView
import com.project.chat.domain.models.ChatMessage
import com.project.chat.domain.models.ChatMessageDeliveryStatus
import com.project.chat.domain.models.MessageAttachment
import com.project.chat.domain.models.OutgoingNewMessage
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Attachments are persisted on [ChatMessageEntity] as a JSON-encoded `List<ChatAttachmentDto>` string.
 * This file-private [Json] handles that (de)serialization; decoding never throws (falls back to empty).
 */
private val attachmentsJson = Json {
    ignoreUnknownKeys = true
}

private fun List<ChatAttachmentDto>.encode(): String = attachmentsJson.encodeToString(this)

private fun String.decodeAttachments(): List<ChatAttachmentDto> {
    return runCatching {
        attachmentsJson.decodeFromString<List<ChatAttachmentDto>>(this)
    }.getOrElse { emptyList() }
}

fun ChatAttachmentDto.toDomain(): MessageAttachment {
    return MessageAttachment(
        storageUrl = storageUrl,
        mimeType = mimeType,
        fileName = originalFileName,
        sizeInBytes = sizeInBytes,
        durationInSeconds = durationInSeconds,
    )
}

fun MessageAttachment.toDto(): ChatAttachmentDto {
    return ChatAttachmentDto(
        storageUrl = storageUrl,
        mimeType = mimeType,
        originalFileName = fileName,
        sizeInBytes = sizeInBytes,
        durationInSeconds = durationInSeconds,
    )
}

fun MessageAttachment.toInput(): AttachmentInput {
    return AttachmentInput(
        storageUrl = storageUrl,
        mimeType = mimeType,
        originalFileName = fileName,
        sizeInBytes = sizeInBytes,
        durationInSeconds = durationInSeconds,
    )
}

fun AttachmentInput.toDto(): ChatAttachmentDto {
    return ChatAttachmentDto(
        storageUrl = storageUrl,
        mimeType = mimeType,
        originalFileName = originalFileName,
        sizeInBytes = sizeInBytes,
        durationInSeconds = durationInSeconds,
    )
}

fun ChatMessageDto.toDomain(): ChatMessage {
    return ChatMessage(
        id = id,
        chatId = chatId,
        content = content,
        createdAt = Instant.parse(createdAt),
        senderId = senderId,
        deliveryStatus = ChatMessageDeliveryStatus.SENT,
        attachments = attachments.map { it.toDomain() },
    )
}

fun ChatMessageEntity.toDomain(): ChatMessage {
    return ChatMessage(
        id = messageId,
        chatId = chatId,
        content = content,
        createdAt = Instant.fromEpochMilliseconds(timestamp),
        senderId = senderId,
        deliveryStatus = ChatMessageDeliveryStatus.valueOf(deliveryStatus),
        attachments = attachments.decodeAttachments().map { it.toDomain() },
    )
}

fun LastMessageView.toDomain(): ChatMessage {
    return ChatMessage(
        id = messageId,
        chatId = chatId,
        content = content,
        createdAt = Instant.fromEpochMilliseconds(timestamp),
        senderId = senderId,
        deliveryStatus = ChatMessageDeliveryStatus.valueOf(this.deliveryStatus),
    )
}

fun ChatMessage.toEntity(): ChatMessageEntity {
    return ChatMessageEntity(
        messageId = id,
        chatId = chatId,
        senderId = senderId,
        content = content,
        timestamp = createdAt.toEpochMilliseconds(),
        deliveryStatus = deliveryStatus.name,
        attachments = attachments.map { it.toDto() }.encode(),
    )
}

fun ChatMessage.toLastMessageView(): LastMessageView {
    return LastMessageView(
        messageId = id,
        chatId = chatId,
        senderId = senderId,
        content = content,
        timestamp = createdAt.toEpochMilliseconds(),
        deliveryStatus = deliveryStatus.name,
        senderUsername = null,
    )
}

fun ChatMessage.toNewMessage(): OutgoingWebSocketDto.NewMessage {
    return OutgoingWebSocketDto.NewMessage(
        messageId = id,
        chatId = chatId,
        content = content,
        attachments = attachments.map { it.toInput() },
    )
}

fun IncomingWebSocketDto.NewMessageDto.toEntity(): ChatMessageEntity {
    return ChatMessageEntity(
        messageId = id,
        chatId = chatId,
        senderId = senderId,
        content = content,
        timestamp = Instant.parse(createdAt).toEpochMilliseconds(),
        deliveryStatus = ChatMessageDeliveryStatus.SENT.name,
        attachments = attachments.encode(),
    )
}

fun OutgoingNewMessage.toWebSocketDto(): OutgoingWebSocketDto.NewMessage {
    return OutgoingWebSocketDto.NewMessage(
        chatId = chatId,
        messageId = messageId,
        content = content,
        attachments = attachments.map { it.toInput() },
    )
}

fun OutgoingWebSocketDto.NewMessage.toEntity(
    senderId: String,
    deliveryStatus: ChatMessageDeliveryStatus,
): ChatMessageEntity {
    return ChatMessageEntity(
        messageId = messageId,
        chatId = chatId,
        content = content,
        senderId = senderId,
        deliveryStatus = deliveryStatus.name,
        timestamp = Clock.System.now().toEpochMilliseconds(),
        attachments = attachments.map { it.toDto() }.encode(),
    )
}

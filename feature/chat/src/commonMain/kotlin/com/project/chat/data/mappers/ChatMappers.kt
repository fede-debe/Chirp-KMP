package com.project.chat.data.mappers

import com.project.chat.data.dto.ChatDto
import com.project.chat.database.entities.ChatEntity
import com.project.chat.database.entities.ChatInfoEntity
import com.project.chat.database.entities.ChatWithParticipants
import com.project.chat.database.entities.MessageWithSender
import com.project.chat.domain.models.Chat
import com.project.chat.domain.models.ChatInfo
import com.project.chat.domain.models.ChatMessage
import com.project.chat.domain.models.ChatMessageDeliveryStatus
import com.project.chat.domain.models.ChatParticipant
import kotlin.time.Instant

typealias DataMessageWithSender = MessageWithSender
typealias DomainMessageWithSender = com.project.chat.domain.models.MessageWithSender

fun ChatDto.toDomain(): Chat {
    val lastMessageSenderUsername = lastMessage?.let { message ->
        participants.find { it.userId == message.senderId }?.username
    }
    return Chat(
        id = id,
        participants = participants.map { it.toDomain() },
        lastActivityAt = Instant.parse(lastActivityAt),
        lastMessage = lastMessage?.toDomain(),
        lastMessageSenderUsername = lastMessageSenderUsername,
        creatorId = creator.userId,
    )
}

fun ChatEntity.toDomain(
    participants: List<ChatParticipant>,
    lastMessage: ChatMessage? = null,
): Chat {
    val lastMessageSenderUsername = lastMessage?.let { message ->
        participants.find { it.userId == message.senderId }?.username
    }
    return Chat(
        id = chatId,
        participants = participants,
        lastActivityAt = Instant.fromEpochMilliseconds(lastActivityAt),
        lastMessage = lastMessage,
        lastMessageSenderUsername = lastMessageSenderUsername,
        creatorId = creatorId,
    )
}

fun ChatWithParticipants.toDomain(): Chat {
    return Chat(
        id = chat.chatId,
        participants = participants.map { it.toDomain() },
        lastActivityAt = Instant.fromEpochMilliseconds(chat.lastActivityAt),
        lastMessage = lastMessage?.toDomain(),
        lastMessageSenderUsername = lastMessage?.senderUsername,
        creatorId = chat.creatorId,
    )
}

fun Chat.toEntity(): ChatEntity {
    return ChatEntity(
        chatId = id,
        lastActivityAt = lastActivityAt.toEpochMilliseconds(),
        creatorId = creatorId,
    )
}

fun DataMessageWithSender.toDomain(): DomainMessageWithSender {
    return DomainMessageWithSender(
        message = message.toDomain(),
        sender = sender.toDomain(),
        deliveryStatus = ChatMessageDeliveryStatus.valueOf(this.message.deliveryStatus),
    )
}

fun ChatInfoEntity.toDomain(): ChatInfo {
    return ChatInfo(
        chat = chat.toDomain(
            participants = this.participants.map { it.toDomain() },
        ),
        messages = messagesWithSenders.map { it.toDomain() },
    )
}

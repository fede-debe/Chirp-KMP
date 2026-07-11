package com.project.chat.data.mappers

import com.project.chat.data.dto.websocket.IncomingWebSocketDto
import com.project.chat.domain.models.TypingUser

fun IncomingWebSocketDto.TypingIndicatorDto.toDomain(): TypingUser = TypingUser(
    chatId = chatId,
    userId = userId,
    username = username,
    isTyping = isTyping,
)

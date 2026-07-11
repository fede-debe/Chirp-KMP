package com.project.chat.presentation.mappers

import com.project.chat.domain.models.MessageAttachment
import com.project.chat.domain.models.MessageWithSender
import com.project.chat.presentation.models.MessageAttachmentUi
import com.project.chat.presentation.models.MessageUi
import com.project.chat.presentation.util.DateUtils
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun List<MessageWithSender>.toUiList(localUserId: String): List<MessageUi> {
    return this
        .sortedByDescending { it.message.createdAt }
        .groupBy {
            it.message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
        }
        .flatMap { (date, messages) ->
            messages.map { it.toUi(localUserId) } + MessageUi.DateSeparator(
                id = date.toString(),
                date = DateUtils.formatDateSeparator(date),
            )
        }
}

fun MessageWithSender.toUi(
    localUserId: String,
): MessageUi {
    val isFromLocalUser = this.sender.userId == localUserId
    val attachmentsUi = message.attachments.map { it.toUi() }
    return if (isFromLocalUser) {
        MessageUi.LocalUserMessage(
            id = message.id,
            content = message.content,
            deliveryStatus = message.deliveryStatus,
            formattedSentTime = DateUtils.formatMessageTime(instant = message.createdAt),
            attachments = attachmentsUi,
        )
    } else {
        MessageUi.OtherUserMessage(
            id = message.id,
            content = message.content,
            formattedSentTime = DateUtils.formatMessageTime(instant = message.createdAt),
            sender = sender.toUi(),
            attachments = attachmentsUi,
        )
    }
}

private fun MessageAttachment.toUi(): MessageAttachmentUi {
    return MessageAttachmentUi(
        url = storageUrl,
        fileName = fileName,
        mimeType = mimeType,
        durationInSeconds = durationInSeconds,
    )
}

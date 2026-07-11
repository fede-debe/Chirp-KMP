package com.project.chat.presentation.models

/**
 * A sent/received image attachment as shown in a message bubble.
 *
 * @param url permanent storage URL loaded by Coil for the thumbnail/full-screen view.
 */
data class MessageAttachmentUi(
    val url: String,
    val fileName: String,
    val mimeType: String,
    val durationInSeconds: Int? = null,
)

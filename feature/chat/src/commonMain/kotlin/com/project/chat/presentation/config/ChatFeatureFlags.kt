package com.project.chat.presentation.config

import com.project.feature.chat.BuildKonfig

/**
 * Single source of truth for the chat sub-feature gates (send-side). Values come from BuildKonfig
 * (local.properties, default false) and are only meaningful when CHAT_ENABLED is true. UI reads
 * these instead of touching BuildKonfig directly.
 */
object ChatFeatureFlags {
    val voice: Boolean = BuildKonfig.CHAT_VOICE_ENABLED
    val typing: Boolean = BuildKonfig.CHAT_TYPING_ENABLED
    val attachments: Boolean = BuildKonfig.CHAT_ATTACHMENTS_ENABLED
    val admin: Boolean = BuildKonfig.CHAT_ADMIN_ENABLED
}

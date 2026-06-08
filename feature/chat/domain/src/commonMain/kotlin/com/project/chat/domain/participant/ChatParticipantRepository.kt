package com.project.chat.domain.participant

import com.project.chat.domain.models.ChatParticipant
import com.project.core.domain.util.DataError
import com.project.core.domain.util.Result

interface ChatParticipantRepository {
    suspend fun fetchLocalParticipant(): Result<ChatParticipant, DataError>
}

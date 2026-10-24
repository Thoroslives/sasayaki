package com.sasayaki.domain.transcription

import com.sasayaki.domain.provider.Endpoint
import java.io.File

interface TranscriptionEngine {
    suspend fun transcribe(audioFile: File, endpoint: Endpoint, language: String?, prompt: String?): Result<String>
    fun isAvailable(): Boolean
}

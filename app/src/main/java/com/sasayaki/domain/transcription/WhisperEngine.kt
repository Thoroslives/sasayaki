package com.sasayaki.domain.transcription

import com.sasayaki.data.api.ApiClientFactory
import com.sasayaki.data.api.AsrApiService
import com.sasayaki.domain.provider.Endpoint
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhisperEngine @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val networkMonitor: NetworkMonitor
) : TranscriptionEngine {

    override suspend fun transcribe(audioFile: File, endpoint: Endpoint, language: String?, prompt: String?): Result<String> {
        return try {
            val service = apiClientFactory.create(AsrApiService::class.java, endpoint.baseUrl, endpoint.apiKey)

            val filePart = MultipartBody.Part.createFormData(
                "file",
                audioFile.name,
                audioFile.asRequestBody("audio/wav".toMediaType())
            )
            val modelPart = endpoint.model.toRequestBody("text/plain".toMediaType())
            val languagePart = language?.let {
                it.toRequestBody("text/plain".toMediaType())
            }

            val promptPart = prompt?.toRequestBody("text/plain".toMediaType())

            val response = service.transcribe(filePart, modelPart, prompt = promptPart, language = languagePart)
            Result.success(response.text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isAvailable(): Boolean {
        return networkMonitor.isOnline.value
    }
}

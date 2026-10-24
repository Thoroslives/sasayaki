package com.sasayaki.domain.transcription

import com.sasayaki.data.api.ApiClientFactory
import com.sasayaki.data.api.AsrApiService
import com.sasayaki.domain.provider.Endpoint
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
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

            val response = try {
                service.transcribe(filePart, modelPart, prompt = promptPart, language = languagePart)
            } catch (e: HttpException) {
                // A server that refuses the glossary should still transcribe without it.
                if (promptPart == null || e.code() !in REFUSED_PROMPT_CODES) throw e
                service.transcribe(filePart, modelPart, language = languagePart)
            }
            Result.success(response.text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        val REFUSED_PROMPT_CODES = setOf(400, 422)
    }

    override fun isAvailable(): Boolean {
        return networkMonitor.isOnline.value
    }
}

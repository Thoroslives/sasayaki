package com.sasayaki.domain.provider

import com.sasayaki.data.api.ApiClientFactory
import com.sasayaki.data.api.AsrApiService
import com.sasayaki.data.api.LlmApiService
import com.sasayaki.data.api.ModelsApiService
import com.sasayaki.data.api.model.ChatCompletionRequest
import com.sasayaki.data.api.model.ChatMessage
import com.sasayaki.data.preferences.PreferencesDataStore
import com.sasayaki.data.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

enum class ProviderKind { SPEECH, CLEANUP }

sealed interface ConnectionResult {
    /** [models] is empty when the server works but does not publish a model list. */
    data class Connected(val models: List<String>) : ConnectionResult
    data class Failed(val message: String) : ConnectionResult
}

@Singleton
class ProviderRepository @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val preferencesDataStore: PreferencesDataStore,
    private val profileRepository: ProfileRepository
) {
    private val _models = MutableStateFlow<Map<ProviderKind, List<String>>>(emptyMap())
    val models: StateFlow<Map<ProviderKind, List<String>>> = _models.asStateFlow()

    suspend fun test(kind: ProviderKind, baseUrl: String, apiKey: String): ConnectionResult = try {
        val models = listModels(baseUrl, apiKey)
        _models.update { it + (kind to models) }
        ConnectionResult.Connected(models)
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        // Not every OpenAI-compatible server implements /v1/models, so fall back to
        // exercising the endpoint dictation actually uses.
        if (e.code() in MODELS_UNSUPPORTED) probe(kind, baseUrl, apiKey) else ConnectionResult.Failed(describeConnectionError(e))
    } catch (e: Exception) {
        ConnectionResult.Failed(describeConnectionError(e))
    }

    /** Fetches models for the saved endpoint of [kind]; an empty list when unavailable. */
    suspend fun refreshModels(kind: ProviderKind): List<String> {
        val prefs = preferencesDataStore.preferences.first()
        val (url, key) = when (kind) {
            ProviderKind.SPEECH -> prefs.asrBaseUrl to prefs.asrApiKey
            ProviderKind.CLEANUP -> prefs.llmBaseUrl to prefs.llmApiKey
        }
        if (url.isBlank()) return emptyList()
        val models = runCatching { listModels(url, key) }
            .onFailure { if (it is CancellationException) throw it }
            .getOrDefault(emptyList())
        _models.update { it + (kind to models) }
        return models
    }

    private suspend fun listModels(baseUrl: String, apiKey: String): List<String> =
        apiClientFactory.create(ModelsApiService::class.java, baseUrl, apiKey)
            .listModels()
            .data.orEmpty()
            .mapNotNull { it.id?.takeIf(String::isNotBlank) }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)

    private suspend fun probe(kind: ProviderKind, baseUrl: String, apiKey: String): ConnectionResult = try {
        val profile = profileRepository.getActiveProfile()
        when (kind) {
            ProviderKind.SPEECH -> {
                val service = apiClientFactory.create(AsrApiService::class.java, baseUrl, apiKey)
                val file = MultipartBody.Part.createFormData("file", "probe.wav", silentWav().toRequestBody(WAV))
                service.transcribe(file, profile.asrModel.toRequestBody(TEXT))
            }
            ProviderKind.CLEANUP -> {
                val service = apiClientFactory.create(LlmApiService::class.java, baseUrl, apiKey)
                service.chatCompletion(ChatCompletionRequest(profile.llmModel, listOf(ChatMessage("user", "Reply with OK."))))
            }
        }
        ConnectionResult.Connected(emptyList())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ConnectionResult.Failed(describeConnectionError(e))
    }

    private companion object {
        val MODELS_UNSUPPORTED = setOf(404, 405, 501)
        val WAV = "audio/wav".toMediaType()
        val TEXT = "text/plain".toMediaType()

        /** A tenth of a second of 16 kHz mono silence: the smallest upload every server accepts. */
        fun silentWav(): ByteArray {
            val sampleRate = 16_000
            val dataSize = sampleRate / 10 * 2
            return ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray()); putInt(36 + dataSize); put("WAVE".toByteArray())
                put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
                putInt(sampleRate); putInt(sampleRate * 2); putShort(2); putShort(16)
                put("data".toByteArray()); putInt(dataSize)
            }.array()
        }
    }
}

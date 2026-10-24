package com.sasayaki.domain.processing

import android.util.Log
import com.sasayaki.data.api.ApiClientFactory
import com.sasayaki.data.api.LlmApiService
import com.sasayaki.data.api.model.ChatCompletionRequest
import com.sasayaki.data.api.model.ChatCompletionResponse
import com.sasayaki.data.repository.ProcessingRepository
import com.sasayaki.data.repository.ProviderRepository
import com.sasayaki.domain.provider.Purpose
import com.sasayaki.domain.provider.Resolution
import com.sasayaki.domain.model.AppContext
import com.sasayaki.domain.model.Profile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextProcessor @Inject constructor(
    private val apiClientFactory: ApiClientFactory,
    private val providerRepository: ProviderRepository,
    private val processingRepository: ProcessingRepository
) {
    /**
     * @param onPostProcessing invoked immediately before the LLM request, so callers can
     *   distinguish the network-bound post-processing phase from transcription.
     */
    suspend fun process(
        rawText: String,
        profile: Profile,
        appContext: AppContext? = null,
        skipCleanup: Boolean = false,
        onPostProcessing: (() -> Unit)? = null
    ): String {
        val ruleProcessedText = try {
            processingRepository.applySelectedRules(rawText, profile.selectedRuleIds)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Replacement rules failed", e)
            rawText
        }

        return try {
            if (!profile.llmEnabled || skipCleanup || ruleProcessedText.isBlank()) return ruleProcessedText
            val endpoint = (providerRepository.resolve(profile, Purpose.CLEANUP) as? Resolution.Ready)?.endpoint
                ?: return ruleProcessedText

            onPostProcessing?.invoke()

            val service = apiClientFactory.create(LlmApiService::class.java, endpoint.baseUrl, endpoint.apiKey)

            val systemPrompt = processingRepository.buildSystemPrompt(profile, appContext)

            val response = complete(service, "${endpoint.baseUrl}|${endpoint.model}") { hints ->
                cleanupRequest(endpoint.model, systemPrompt, ruleProcessedText, profile.outputStyle, hints)
            }
            val cleaned = stripModelArtifacts(response.text, ruleProcessedText)
            val rejection = rejectCleanup(ruleProcessedText, cleaned, response.finishReason, lengthCheckFor(profile))
            if (rejection != null) {
                Log.w(TAG, "Cleanup output rejected ($rejection); using rule-processed text")
                ruleProcessedText
            } else {
                cleaned
            }
        } catch (e: CancellationException) {
            // The user cancelled mid-request; never fall through to injecting text.
            throw e
        } catch (e: Exception) {
            // Fall back to the rule-processed text rather than the raw transcript, so a
            // failed LLM call does not also discard the local replacement rules.
            Log.e(TAG, "Post-processing failed; using rule-processed text", e)
            ruleProcessedText
        }
    }

    private suspend fun complete(
        service: LlmApiService,
        serverModel: String,
        request: (ModelHints) -> ChatCompletionRequest
    ): ChatCompletionResponse {
        var rung = acceptedHints[serverModel] ?: 0
        while (true) {
            try {
                return service.chatCompletion(request(ModelHints.LADDER[rung])).also { acceptedHints[serverModel] = rung }
            } catch (e: HttpException) {
                if (e.code() !in REJECTED_REQUEST_CODES || rung == ModelHints.LADDER.lastIndex) throw e
                rung++
            }
        }
    }

    // acceptedHints remembers per server and model the first rung of [ModelHints.LADDER]
    // that was not rejected, so only the first cleanup pays for the probing.
    private val acceptedHints = ConcurrentHashMap<String, Int>()

    private companion object {
        const val TAG = "TextProcessor"
        val REJECTED_REQUEST_CODES = setOf(400, 422)
    }
}

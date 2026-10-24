package com.sasayaki.domain.processing

import com.google.gson.Gson
import com.sasayaki.data.api.model.ChatCompletionResponse
import com.sasayaki.domain.model.OutputStyle
import com.sasayaki.domain.model.PostProcessingPrompt
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

data class Completion(
    val text: String,
    val latencyMs: Long,
    val error: String? = null,
    val raw: String = text,
    val finishReason: String? = null
)

/**
 * Shared plumbing for the two LLM-backed suites.
 *
 * Everything is configured from the environment and nothing is logged, so credentials do
 * not reach the report. Requests are issued one at a time: running them concurrently
 * would make the latency figures a measure of the connection pool rather than the model.
 */
class BenchmarkClient {
    private val endpoint: String? = System.getenv("OPENAI_ENDPOINT")?.trimEnd('/')
    private val apiKey: String? = System.getenv("OPENAI_API_KEY")

    val models: List<String> = (
        System.getenv("BENCH_MODELS") ?: System.getenv("OPENAI_MODEL") ?: ""
        ).split(",").map(String::trim).filter(String::isNotEmpty)

    /** Repetitions per measurement. Outputs vary run to run, so one sample proves little. */
    val reps: Int = System.getenv("BENCH_REPS")?.toIntOrNull()?.coerceAtLeast(1) ?: 3

    val isConfigured: Boolean
        get() = !endpoint.isNullOrBlank() && !apiKey.isNullOrBlank() && models.isNotEmpty()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    val builtInPrompts: List<PostProcessingPrompt> =
        BuiltInPrompts.ALL.mapIndexed { index, seed ->
            PostProcessingPrompt(
                id = index + 1L,
                title = seed.title,
                prompt = seed.prompt,
                builtIn = true
            )
        }

    private class Refused(val code: Int, val body: String) : Exception("HTTP $code")

    // optionalFields is the app's own memory of which optional fields a server refuses.
    private val optionalFields = OptionalFieldMemory()

    /**
     * complete sends the request the app sends, including the probing for optional fields,
     * and post-processes the answer the same way. [Completion.raw] keeps the unprocessed
     * answer so leaked framing can be counted.
     */
    fun complete(model: String, systemPrompt: String, userText: String, style: OutputStyle): Completion {
        var text = ""
        var raw = ""
        var finishReason: String? = null
        var failure: String? = null
        val latency = measureTimeMillis {
            try {
                val parsed = runBlocking {
                    optionalFields.send(model, { e -> (e as? Refused)?.takeIf { it.code in setOf(400, 422) }?.body }) { omitted ->
                        val payload = cleanupRequest(model, systemPrompt, userText, style, withExamples = true, omitted = omitted)
                        val http = Request.Builder()
                            .url("$endpoint/chat/completions")
                            .addHeader("Authorization", "Bearer $apiKey")
                            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
                            .build()
                        client.newCall(http).execute().use { response ->
                            val body = response.body?.string().orEmpty()
                            if (!response.isSuccessful) throw Refused(response.code, body)
                            gson.fromJson(body, ChatCompletionResponse::class.java)
                        }
                    }
                }
                raw = parsed.text
                finishReason = parsed.finishReason
                text = stripModelArtifacts(raw, userText)
            } catch (e: Refused) {
                failure = "HTTP ${e.code}"
            } catch (e: Exception) {
                failure = e::class.simpleName + ": " + (e.message ?: "")
            }
        }
        return Completion(text, latency, failure, raw, finishReason)
    }

    fun completeRepeatedly(model: String, systemPrompt: String, userText: String, style: OutputStyle): List<Completion> =
        (1..reps).map { complete(model, systemPrompt, userText, style) }
}

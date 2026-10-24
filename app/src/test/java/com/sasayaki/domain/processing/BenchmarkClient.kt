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

    // hintRungs mirrors the app's per-model memory of which optional fields a server accepts.
    private val hintRungs = mutableMapOf<String, Int>()

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
                var rung = hintRungs[model] ?: 0
                while (true) {
                    val payload = cleanupRequest(model, systemPrompt, userText, style, ModelHints.LADDER[rung])
                    val http = Request.Builder()
                        .url("$endpoint/chat/completions")
                        .addHeader("Authorization", "Bearer $apiKey")
                        .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
                        .build()
                    val code = client.newCall(http).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (response.isSuccessful) {
                            val parsed = gson.fromJson(body, ChatCompletionResponse::class.java)
                            raw = parsed.text
                            finishReason = parsed.finishReason
                            text = stripModelArtifacts(raw, userText)
                        }
                        response.code
                    }
                    if (code in 200..299) {
                        hintRungs[model] = rung
                        break
                    }
                    if (code !in setOf(400, 422) || rung == ModelHints.LADDER.lastIndex) {
                        failure = "HTTP $code"
                        break
                    }
                    rung++
                }
            } catch (e: Exception) {
                failure = e::class.simpleName + ": " + (e.message ?: "")
            }
        }
        return Completion(text, latency, failure, raw, finishReason)
    }

    fun completeRepeatedly(model: String, systemPrompt: String, userText: String, style: OutputStyle): List<Completion> =
        (1..reps).map { complete(model, systemPrompt, userText, style) }
}

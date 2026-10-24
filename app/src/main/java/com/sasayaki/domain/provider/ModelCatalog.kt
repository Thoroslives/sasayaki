package com.sasayaki.domain.provider

import androidx.compose.runtime.Immutable

enum class ModelKind { SPEECH, CHAT, OTHER }

@Immutable
data class ModelInfo(val id: String, val kind: ModelKind)

private val speechHints = listOf(
    "whisper", "transcribe", "parakeet", "canary", "voxtral", "moonshine",
    "sensevoice", "wav2vec", "speech-to-text", "-stt", "stt-", "asr"
)
private val otherHints = listOf(
    "tts", "kokoro", "piper", "embed", "rerank", "moderation", "dall-e", "image", "vision-encoder"
)

/**
 * classifyModel guesses a model's kind from its id, since OpenAI-compatible /v1/models
 * carries no capability field. [task] is honoured when a server provides one (speaches does).
 */
fun classifyModel(id: String, task: String? = null): ModelKind {
    task?.lowercase()?.let { hint ->
        when {
            "speech-recognition" in hint || hint == "asr" -> return ModelKind.SPEECH
            "text-to-speech" in hint || "embedding" in hint -> return ModelKind.OTHER
        }
    }
    val name = id.lowercase()
    return when {
        otherHints.any { it in name } -> ModelKind.OTHER
        speechHints.any { it in name } -> ModelKind.SPEECH
        else -> ModelKind.CHAT
    }
}

/** suitableFor returns the models suited to [purpose], or all of them when none look suitable, so a bad guess hides nothing. */
fun List<ModelInfo>.suitableFor(purpose: Purpose): List<String> {
    val wanted = if (purpose == Purpose.SPEECH) ModelKind.SPEECH else ModelKind.CHAT
    return filter { it.kind == wanted }.map(ModelInfo::id).ifEmpty { map(ModelInfo::id) }
}

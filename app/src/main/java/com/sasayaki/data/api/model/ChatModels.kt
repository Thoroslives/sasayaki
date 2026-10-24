package com.sasayaki.data.api.model

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("temperature") val temperature: Double? = null,
    @SerializedName("max_tokens") val maxTokens: Int? = null,
    @SerializedName("reasoning_effort") val reasoningEffort: String? = null,
    @SerializedName("chat_template_kwargs") val chatTemplateKwargs: Map<String, Any>? = null
)

data class ChatMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class ChatCompletionResponse(
    @SerializedName("choices") val choices: List<Choice>
) {
    val text: String get() = choices.firstOrNull()?.message?.content ?: ""
    val finishReason: String? get() = choices.firstOrNull()?.finishReason
}

data class Choice(
    @SerializedName("index") val index: Int,
    @SerializedName("message") val message: ChatMessage,
    @SerializedName("finish_reason") val finishReason: String? = null
)

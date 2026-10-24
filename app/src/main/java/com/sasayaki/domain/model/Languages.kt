package com.sasayaki.domain.model

import java.util.Locale

// speechLanguageCodes are the ISO 639 codes Whisper-family models accept for the
// transcription language parameter.
private val speechLanguageCodes = listOf(
    "af", "am", "ar", "as", "az", "ba", "be", "bg", "bn", "bo", "br", "bs", "ca", "cs", "cy", "da", "de",
    "el", "en", "es", "et", "eu", "fa", "fi", "fo", "fr", "gl", "gu", "ha", "haw", "he", "hi", "hr", "ht",
    "hu", "hy", "id", "is", "it", "ja", "jw", "ka", "kk", "km", "kn", "ko", "la", "lb", "ln", "lo", "lt",
    "lv", "mg", "mi", "mk", "ml", "mn", "mr", "ms", "mt", "my", "ne", "nl", "nn", "no", "oc", "pa", "pl",
    "ps", "pt", "ro", "ru", "sa", "sd", "si", "sk", "sl", "sn", "so", "sq", "sr", "su", "sv", "sw", "ta",
    "te", "tg", "th", "tk", "tl", "tr", "tt", "uk", "ur", "uz", "vi", "yi", "yo", "yue", "zh"
)

data class SpeechLanguage(val code: String, val name: String)

fun speechLanguages(displayLocale: Locale = Locale.getDefault()): List<SpeechLanguage> =
    speechLanguageCodes
        .map { SpeechLanguage(it, languageName(it, displayLocale)) }
        .sortedBy { it.name.lowercase(displayLocale) }

fun languageName(code: String, displayLocale: Locale = Locale.getDefault()): String {
    val name = Locale.forLanguageTag(code).getDisplayLanguage(displayLocale)
    return if (name.isBlank() || name == code) code.uppercase(displayLocale)
    else name.replaceFirstChar { it.titlecase(displayLocale) }
}

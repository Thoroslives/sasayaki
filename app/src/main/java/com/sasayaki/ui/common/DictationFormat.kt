package com.sasayaki.ui.common

import java.util.Locale
import kotlin.math.roundToInt

// TYPING_WORDS_PER_MINUTE is a typical touch-keyboard speed; time saved compares against it.
private const val TYPING_WORDS_PER_MINUTE = 40.0
private const val MAX_APP_LABEL_LENGTH = 24

private val knownApps = listOf(
    "whatsapp" to "WhatsApp",
    "signal" to "Signal",
    "molly" to "Molly",
    "element" to "Element",
    "telegram" to "Telegram",
    "gmail" to "Gmail",
    "outlook" to "Outlook",
    "discord" to "Discord",
    "slack" to "Slack",
    "securesms" to "Signal"
)

private val genericPackageSegments = setOf(
    "android", "app", "apps", "client", "com", "debug", "im", "io", "mobile", "net", "org", "release", "x"
)

fun displaySourceApp(sourceApp: String?, sourceAppPackage: String?): String? {
    val label = sourceApp?.trim()?.takeIf { it.isNotBlank() && !it.equals("App", ignoreCase = true) }
    if (label != null && !label.contains('.')) return label.take(MAX_APP_LABEL_LENGTH)

    val packageName = sourceAppPackage?.trim()?.takeIf { it.isNotBlank() }
        ?: label?.takeIf { it.contains('.') }
        ?: return label?.take(MAX_APP_LABEL_LENGTH)

    knownApps.firstOrNull { (fragment, _) -> packageName.contains(fragment, ignoreCase = true) }
        ?.let { return it.second }
    return appLabelFromPackage(packageName).take(MAX_APP_LABEL_LENGTH)
}

private fun appLabelFromPackage(packageName: String): String {
    val segment = packageName.split('.')
        .firstOrNull { it.length > 1 && it.lowercase() !in genericPackageSegments }
        ?: packageName.substringAfterLast('.')
    return segment.replace('_', ' ').replace('-', ' ').trim().replaceFirstChar { it.uppercase() }
}

fun formatCompactNumber(number: Int): String = when {
    number >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", number / 1_000_000.0)
    number >= 10_000 -> String.format(Locale.getDefault(), "%.1fK", number / 1_000.0)
    else -> number.toString()
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

fun minutesSaved(words: Int, speakingMs: Long): Int {
    val typingMinutes = words / TYPING_WORDS_PER_MINUTE
    val speakingMinutes = speakingMs / 60_000.0
    return (typingMinutes - speakingMinutes).coerceAtLeast(0.0).roundToInt()
}

fun wordsPerMinute(words: Int, speakingMs: Long): Int? {
    if (words == 0 || speakingMs < 1000) return null
    return (words / (speakingMs / 60_000.0)).roundToInt()
}

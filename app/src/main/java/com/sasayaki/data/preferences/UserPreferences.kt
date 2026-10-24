package com.sasayaki.data.preferences

data class UserPreferences(
    val autoClipboard: Boolean = true,
    val vibrateOnRecord: Boolean = true,
    val pauseOtherAudio: Boolean = false,
    val silenceThresholdMs: Long = 2000,
    val preferredLanguages: List<String> = emptyList(),
    val activeLanguage: String? = null,
    val historyEnabled: Boolean = true,
    val keepStatsWithoutHistory: Boolean = false,
    val historyRetentionLimit: Int = 500,
    val startOnBoot: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val wallpaperColors: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val bubbleSize: BubbleSize = BubbleSize.STANDARD,
    val bubbleTranslucent: Boolean = false,
    val bubbleMonochrome: Boolean = false,
    val rememberProfilePerApp: Boolean = true,
    val skipCleanupInAiChats: Boolean = true,
    val appProfiles: Map<String, Long> = emptyMap(), // package name to profile id
    val vocabulary: List<String> = emptyList()
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class BubbleSize(val scale: Float) {
    STANDARD(1f),
    LARGE(1.15f),
    LARGER(1.3f)
}

private const val APP_PROFILE_SEPARATOR = '='

internal fun encodeAppProfiles(profiles: Map<String, Long>): Set<String> =
    profiles.map { (packageName, id) -> "$packageName$APP_PROFILE_SEPARATOR$id" }.toSet()

internal fun decodeAppProfiles(entries: Set<String>): Map<String, Long> = entries.mapNotNull { entry ->
    val packageName = entry.substringBeforeLast(APP_PROFILE_SEPARATOR, missingDelimiterValue = "")
    val id = entry.substringAfterLast(APP_PROFILE_SEPARATOR).toLongOrNull()
    if (packageName.isEmpty() || id == null) null else packageName to id
}.toMap()

fun UserPreferences.rememberedProfileFor(packageName: String?): Long? =
    packageName?.takeIf { rememberProfilePerApp }?.let(appProfiles::get)

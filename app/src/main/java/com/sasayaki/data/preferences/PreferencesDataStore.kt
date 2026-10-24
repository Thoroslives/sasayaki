package com.sasayaki.data.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sasayaki.domain.provider.LegacyEndpoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferencesStore: SecurePreferencesStore
) {
    private object Keys {
        val ASR_BASE_URL = stringPreferencesKey("asr_base_url")
        val ASR_API_KEY = stringPreferencesKey("asr_api_key")
        val ASR_MODEL = stringPreferencesKey("asr_model")
        val LLM_BASE_URL = stringPreferencesKey("llm_base_url")
        val LLM_API_KEY = stringPreferencesKey("llm_api_key")
        val LLM_MODEL = stringPreferencesKey("llm_model")
        val LLM_ENABLED = booleanPreferencesKey("llm_enabled")
        val AUTO_CLIPBOARD = booleanPreferencesKey("auto_clipboard")
        val VIBRATE_ON_RECORD = booleanPreferencesKey("vibrate_on_record")
        val PAUSE_OTHER_AUDIO = booleanPreferencesKey("pause_other_audio")
        val SILENCE_THRESHOLD_MS = longPreferencesKey("silence_threshold_ms")
        val LANGUAGE = stringPreferencesKey("language")
        val PREFERRED_LANGUAGES = stringPreferencesKey("preferred_languages")
        val ACTIVE_LANGUAGE = stringPreferencesKey("active_language")
        val HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val KEEP_STATS_WITHOUT_HISTORY = booleanPreferencesKey("keep_stats_without_history")
        val HISTORY_RETENTION_LIMIT = intPreferencesKey("history_retention_limit")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WALLPAPER_COLORS = booleanPreferencesKey("wallpaper_colors")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val BUBBLE_SIZE = stringPreferencesKey("bubble_size")
        val REMEMBER_PROFILE_PER_APP = booleanPreferencesKey("remember_profile_per_app")
        val SKIP_CLEANUP_IN_AI_CHATS = booleanPreferencesKey("skip_cleanup_in_ai_chats")
        val APP_PROFILES = stringSetPreferencesKey("app_profiles")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("PreferencesDataStore", "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
        UserPreferences(
            autoClipboard = prefs[Keys.AUTO_CLIPBOARD] ?: true,
            vibrateOnRecord = prefs[Keys.VIBRATE_ON_RECORD] ?: true,
            pauseOtherAudio = prefs[Keys.PAUSE_OTHER_AUDIO] ?: false,
            silenceThresholdMs = prefs[Keys.SILENCE_THRESHOLD_MS] ?: 2000L,
            preferredLanguages = parsePreferredLanguages(prefs),
            activeLanguage = resolveActiveLanguage(prefs),
            historyEnabled = prefs[Keys.HISTORY_ENABLED] ?: true,
            keepStatsWithoutHistory = prefs[Keys.KEEP_STATS_WITHOUT_HISTORY] ?: false,
            historyRetentionLimit = (prefs[Keys.HISTORY_RETENTION_LIMIT] ?: 500).coerceIn(1, 5000),
            startOnBoot = prefs[Keys.START_ON_BOOT] ?: true,
            themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[Keys.THEME_MODE] } ?: ThemeMode.SYSTEM,
            wallpaperColors = prefs[Keys.WALLPAPER_COLORS] ?: false,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            bubbleSize = BubbleSize.entries.firstOrNull { it.name == prefs[Keys.BUBBLE_SIZE] } ?: BubbleSize.STANDARD,
            rememberProfilePerApp = prefs[Keys.REMEMBER_PROFILE_PER_APP] ?: true,
            skipCleanupInAiChats = prefs[Keys.SKIP_CLEANUP_IN_AI_CHATS] ?: true,
            appProfiles = decodeAppProfiles(prefs[Keys.APP_PROFILES].orEmpty())
        )
    }
    .distinctUntilChanged()
    .flowOn(Dispatchers.IO)

    /**
     * legacyEndpoints returns the single speech and cleanup endpoints from before
     * providers existed, or null once they have been moved into the providers table.
     */
    suspend fun legacyEndpoints(): Pair<LegacyEndpoint?, LegacyEndpoint?>? {
        val prefs = context.dataStore.data.first()
        val speechUrl = prefs[Keys.ASR_BASE_URL].orEmpty()
        val cleanupUrl = prefs[Keys.LLM_BASE_URL].orEmpty()
        if (speechUrl.isBlank() && cleanupUrl.isBlank()) return null
        val speech = LegacyEndpoint(speechUrl, securePreferencesStore.getAsrApiKey(), prefs[Keys.ASR_MODEL] ?: "whisper-1")
        val cleanup = LegacyEndpoint(cleanupUrl, securePreferencesStore.getLlmApiKey(), prefs[Keys.LLM_MODEL] ?: "gpt-4o-mini")
        return speech to cleanup
    }

    fun clearLegacyApiKeysIfPresent() {
        if (securePreferencesStore.hasLegacyApiKeys()) securePreferencesStore.clearLegacyApiKeys()
    }

    suspend fun clearLegacyEndpoints() {
        securePreferencesStore.clearLegacyApiKeys()
        context.dataStore.edit { prefs ->
            listOf(Keys.ASR_BASE_URL, Keys.ASR_MODEL, Keys.LLM_BASE_URL, Keys.LLM_MODEL).forEach(prefs::remove)
            prefs.remove(Keys.LLM_ENABLED)
        }
    }

    suspend fun setAutoClipboard(enabled: Boolean) = set(Keys.AUTO_CLIPBOARD, enabled)

    suspend fun setVibrateOnRecord(enabled: Boolean) = set(Keys.VIBRATE_ON_RECORD, enabled)

    suspend fun setPauseOtherAudio(enabled: Boolean) = set(Keys.PAUSE_OTHER_AUDIO, enabled)

    suspend fun setSilenceThresholdMs(value: Long) = set(Keys.SILENCE_THRESHOLD_MS, value)

    suspend fun setKeepStatsWithoutHistory(enabled: Boolean) = set(Keys.KEEP_STATS_WITHOUT_HISTORY, enabled)

    suspend fun setHistoryRetentionLimit(limit: Int) = set(Keys.HISTORY_RETENTION_LIMIT, limit.coerceIn(1, 5000))

    suspend fun setStartOnBoot(enabled: Boolean) = set(Keys.START_ON_BOOT, enabled)

    suspend fun setHistoryEnabled(enabled: Boolean) = set(Keys.HISTORY_ENABLED, enabled)

    suspend fun setThemeMode(mode: ThemeMode) = set(Keys.THEME_MODE, mode.name)

    suspend fun setWallpaperColors(enabled: Boolean) = set(Keys.WALLPAPER_COLORS, enabled)

    suspend fun setOnboardingCompleted() = set(Keys.ONBOARDING_COMPLETED, true)

    suspend fun setBubbleSize(size: BubbleSize) = set(Keys.BUBBLE_SIZE, size.name)

    suspend fun setRememberProfilePerApp(enabled: Boolean) = set(Keys.REMEMBER_PROFILE_PER_APP, enabled)

    suspend fun setSkipCleanupInAiChats(enabled: Boolean) = set(Keys.SKIP_CLEANUP_IN_AI_CHATS, enabled)

    suspend fun rememberAppProfile(packageName: String, profileId: Long) {
        context.dataStore.edit { prefs ->
            val profiles = decodeAppProfiles(prefs[Keys.APP_PROFILES].orEmpty())
            if (profiles[packageName] != profileId) {
                prefs[Keys.APP_PROFILES] = encodeAppProfiles(profiles + (packageName to profileId))
            }
        }
    }

    suspend fun forgetAppProfiles() {
        context.dataStore.edit { prefs -> prefs.remove(Keys.APP_PROFILES) }
    }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { prefs -> prefs[key] = value }
    }

    suspend fun runStartupMigrations() {
        migrateLegacySecretsIfNeeded(context.dataStore.data.first())
    }

    private fun resolveActiveLanguage(prefs: Preferences): String? {
        val preferred = parsePreferredLanguages(prefs)
        val active = prefs[Keys.ACTIVE_LANGUAGE]
        if (active != null) return if (active in preferred) active else null
        return if (preferred.size == 1) preferred.first() else null
    }

    private fun parsePreferredLanguages(prefs: Preferences): List<String> {
        val stored = prefs[Keys.PREFERRED_LANGUAGES].orEmpty()
        if (stored.isNotBlank()) {
            return stored.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
        }
        val legacy = prefs[Keys.LANGUAGE].orEmpty().trim().lowercase()
        if (legacy.isNotBlank()) return listOf(legacy)
        return emptyList()
    }

    private suspend fun migrateLegacySecretsIfNeeded(prefs: Preferences) {
        val legacyAsrApiKey = prefs[Keys.ASR_API_KEY].orEmpty()
        val legacyLlmApiKey = prefs[Keys.LLM_API_KEY].orEmpty()

        if (legacyAsrApiKey.isBlank() && legacyLlmApiKey.isBlank()) return

        if (legacyAsrApiKey.isNotBlank() && securePreferencesStore.getAsrApiKey().isBlank()) {
            securePreferencesStore.updateAsrApiKey(legacyAsrApiKey)
        }

        if (legacyLlmApiKey.isNotBlank() && securePreferencesStore.getLlmApiKey().isBlank()) {
            securePreferencesStore.updateLlmApiKey(legacyLlmApiKey)
        }

        context.dataStore.edit { mutablePrefs ->
            mutablePrefs.remove(Keys.ASR_API_KEY)
            mutablePrefs.remove(Keys.LLM_API_KEY)
        }
    }

}

package com.sasayaki.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val FILE_NAME = "secure_settings"
        private const val ASR_API_KEY = "asr_api_key"
        private const val LLM_API_KEY = "llm_api_key"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        EncryptedSharedPreferences.create(
            FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getAsrApiKey(): String = sharedPreferences.getString(ASR_API_KEY, "") ?: ""

    fun getLlmApiKey(): String = sharedPreferences.getString(LLM_API_KEY, "") ?: ""

    fun updateAsrApiKey(apiKey: String) {
        sharedPreferences.edit().putString(ASR_API_KEY, apiKey).apply()
    }

    fun updateLlmApiKey(apiKey: String) {
        sharedPreferences.edit().putString(LLM_API_KEY, apiKey).apply()
    }

    fun clearLegacyApiKeys() {
        sharedPreferences.edit().remove(ASR_API_KEY).remove(LLM_API_KEY).apply()
    }

    fun getProviderApiKey(providerId: Long): String =
        sharedPreferences.getString(providerKey(providerId), "") ?: ""

    // Committed synchronously: callers are on IO, and a key lost to process death after
    // its provider row was written would silently break that provider.
    fun setProviderApiKey(providerId: Long, apiKey: String) {
        sharedPreferences.edit().apply {
            if (apiKey.isEmpty()) remove(providerKey(providerId)) else putString(providerKey(providerId), apiKey)
        }.commit()
    }

    fun hasLegacyApiKeys(): Boolean = getAsrApiKey().isNotEmpty() || getLlmApiKey().isNotEmpty()

    private fun providerKey(providerId: Long) = "provider_api_key_$providerId"
}

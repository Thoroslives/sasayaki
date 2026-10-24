package com.sasayaki.data

import android.util.Log
import com.sasayaki.data.preferences.PreferencesDataStore
import com.sasayaki.data.repository.ProfileRepository
import com.sasayaki.data.repository.ProviderRepository
import com.sasayaki.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async

/**
 * StartupMigrations runs the one-time data upgrades exactly once per process, in the
 * order they depend on: API keys move to encrypted storage before endpoints become
 * providers that read them.
 */
@Singleton
class StartupMigrations @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore,
    private val profileRepository: ProfileRepository,
    private val providerRepository: ProviderRepository,
    @ApplicationScope scope: CoroutineScope
) {
    private val run = scope.async(start = CoroutineStart.LAZY) {
        try {
            preferencesDataStore.runStartupMigrations()
            profileRepository.ensureDefaults()
            providerRepository.migrateLegacyEndpoints()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A failed upgrade is retried on the next launch; the app stays usable meanwhile.
            Log.e(TAG, "Startup migration failed", e)
        }
    }

    suspend fun await() = run.await()

    private companion object {
        const val TAG = "StartupMigrations"
    }
}

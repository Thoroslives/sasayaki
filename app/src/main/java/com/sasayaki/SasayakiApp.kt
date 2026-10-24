package com.sasayaki

import android.app.Application
import android.util.Log
import com.sasayaki.data.StartupMigrations
import com.sasayaki.data.repository.HistoryRepository
import com.sasayaki.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SasayakiApp : Application() {
    @Inject lateinit var startupMigrations: StartupMigrations
    @Inject lateinit var historyRepository: HistoryRepository
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            startupMigrations.await()
            // Also clears audio left behind by a crash between retaining it and saving its row.
            try {
                historyRepository.prune()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "History pruning failed", e)
            }
        }
    }

    private companion object {
        const val TAG = "SasayakiApp"
    }
}

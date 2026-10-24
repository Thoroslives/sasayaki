package com.sasayaki

import android.app.Application
import com.sasayaki.data.StartupMigrations
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SasayakiApp : Application() {
    @Inject lateinit var startupMigrations: StartupMigrations

    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        startupScope.launch { startupMigrations.await() }
    }
}

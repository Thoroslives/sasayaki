package com.sasayaki

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sasayaki.data.preferences.ThemeMode
import com.sasayaki.service.BubbleService
import com.sasayaki.ui.navigation.SasayakiNavGraph
import com.sasayaki.ui.theme.SasayakiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    companion object {
        /** Sent by the Quick Settings tile when Android refused to start the bubble itself. */
        const val ACTION_START_BUBBLE = "com.sasayaki.action.START_BUBBLE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The splash stays until the first screen is known, so onboarding never flashes
        // before Home or the other way round.
        installSplashScreen().setKeepOnScreenCondition { viewModel.startDestination.value == null }
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val appearance by viewModel.appearance.collectAsStateWithLifecycle()
            val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
            val darkTheme = when (appearance.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // enableEdgeToEdge() follows the system night mode, so an in-app theme
            // override would otherwise leave dark icons on a dark status bar.
            DisposableEffect(darkTheme) {
                val barStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose {}
            }

            SasayakiTheme(darkTheme = darkTheme, wallpaperColors = appearance.wallpaperColors) {
                startDestination?.let { SasayakiNavGraph(startDestination = it) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_START_BUBBLE) BubbleService.start(this)
    }
}

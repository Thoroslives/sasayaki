package com.sasayaki.screenshots

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sasayaki.data.db.entity.DictationSummary
import com.sasayaki.ui.history.HistoryScreen
import com.sasayaki.ui.history.HistoryUiState
import com.sasayaki.ui.history.groupByDay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = PHONE, application = Application::class)
class HistoryScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val zone = ZoneId.systemDefault()
    private val now = System.currentTimeMillis()

    private val dictations = listOf(
        DictationSummary(1, "Running ten minutes late, save me a seat and order the usual please.", 13, now - 120_000, "Signal", null, 5_000, "SUCCESS", null, 1, null),
        DictationSummary(2, "", 0, now - 600_000, "Slack", null, 3_000, "FAILURE", "Server unreachable. Check your connection and try again.", 1, "/audio.wav"),
        DictationSummary(3, "Thanks for the notes. I'll review the draft tonight and send comments tomorrow morning.", 15, now - 90_000_000, "Gmail", null, 6_000, "SUCCESS", null, 2, null),
        DictationSummary(4, "Can you pick up bread and oat milk on the way home?", 11, now - 95_000_000, "WhatsApp", null, 4_000, "SUCCESS", null, 1, null)
    )

    @Composable
    private fun history(state: HistoryUiState) = HistoryScreen(
        state = state,
        snackbarHostState = SnackbarHostState(),
        onQueryChange = {},
        onCopy = {},
        onRetry = {},
        onDelete = {},
        onClearAll = {},
        onEnableHistory = {},
        loadRawText = { null }
    )

    @Test
    fun list() = compose.screenshot("history_list_light") {
        history(
            HistoryUiState(
                loaded = true,
                hasHistory = true,
                groups = groupByDay(dictations, zone, LocalDate.now(zone))
            )
        )
    }

    @Test
    fun listDark() = compose.screenshot("history_list_dark", dark = true) {
        history(
            HistoryUiState(
                loaded = true,
                hasHistory = true,
                groups = groupByDay(dictations, zone, LocalDate.now(zone)),
                retryingIds = setOf(2L)
            )
        )
    }

    @Test
    fun emptyAndOff() = compose.screenshot("history_off_light") {
        history(HistoryUiState(loaded = true, historyEnabled = false))
    }
}

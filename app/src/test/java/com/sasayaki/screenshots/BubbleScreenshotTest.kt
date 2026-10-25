package com.sasayaki.screenshots

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sasayaki.data.preferences.BubbleSize
import com.sasayaki.domain.model.Profile
import com.sasayaki.service.bubble.BubbleActions
import com.sasayaki.service.bubble.BubbleAppearance
import com.sasayaki.service.bubble.BubbleContent
import com.sasayaki.service.bubble.BubblePhase
import com.sasayaki.service.bubble.BubbleScales
import com.sasayaki.service.bubble.BubbleTheme
import com.sasayaki.service.bubble.BubbleUiState
import com.sasayaki.service.bubble.DismissTarget
import com.sasayaki.service.bubble.ProfileMenu
import com.sasayaki.service.bubble.mode
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = TALL_PHONE, application = Application::class)
class BubbleScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val actions = BubbleActions({}, {}, {}, {}, {}, {}, {})
    private val levels = List(24) { abs(sin(it * 0.7f)) * (0.3f + (it % 5) * 0.15f) }

    @Composable
    private fun gallery(background: Color, appearance: BubbleAppearance = BubbleAppearance()) {
        BubbleTheme(appearance) {
            Column(
                modifier = Modifier
                    .background(background)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                listOf(
                    BubbleUiState(BubblePhase.Idle),
                    BubbleUiState(BubblePhase.Idle, justInserted = true),
                    BubbleUiState(BubblePhase.Recording(paused = false), elapsedSeconds = 7, profileName = "Chat"),
                    BubbleUiState(BubblePhase.Recording(paused = true), elapsedSeconds = 12, profileName = "Email"),
                    BubbleUiState(BubblePhase.Processing(cleaningUp = false)),
                    BubbleUiState(BubblePhase.Processing(cleaningUp = true)),
                    BubbleUiState(BubblePhase.Failed("The server rejected the API key.", canRetry = true)),
                    BubbleUiState(BubblePhase.NoSpeech)
                ).forEach { state -> Window(state) }
                val tinyBubbleLargeControls = BubbleScales(bubble = BubbleSize.SMALL.scale, controls = BubbleSize.LARGER.scale)
                Window(
                    BubbleUiState(BubblePhase.Recording(paused = false), elapsedSeconds = 3, profileName = "Chat"),
                    tinyBubbleLargeControls
                )
                Window(BubbleUiState(BubblePhase.Idle), tinyBubbleLargeControls)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProfileMenu(listOf(Profile(1, "Chat", isActive = true), Profile(2, "Email"), Profile(3, "Raw notes"))) {}
                    DismissTarget(armed = false)
                    DismissTarget(armed = true)
                }
            }
        }
    }

    // Window draws a state inside the overlay window size the service gives it.
    @Composable
    private fun Window(state: BubbleUiState, scales: BubbleScales = BubbleScales()) {
        val mode = state.phase.mode
        val scale = scales.of(mode)
        Box(
            modifier = Modifier
                .size(mode.windowWidth * scale, mode.windowHeight * scale)
                .border(1.dp, Color(0x55FF00FF))
        ) {
            BubbleContent(state, { levels }, actions, {}, scales = scales)
        }
    }

    @Test
    fun overLightApp() = compose.screenshot("bubble_over_light") { gallery(Color(0xFFF7F7F7)) }

    @Test
    fun overDarkApp() = compose.screenshot("bubble_over_dark") { gallery(Color(0xFF101418)) }

    @Test
    fun translucentOverLightApp() =
        compose.screenshot("bubble_translucent_over_light") { gallery(Color(0xFFF7F7F7), BubbleAppearance(translucent = true)) }

    @Test
    fun monochromeOverDarkApp() =
        compose.screenshot("bubble_monochrome_over_dark") { gallery(Color(0xFF101418), BubbleAppearance(monochrome = true)) }
}

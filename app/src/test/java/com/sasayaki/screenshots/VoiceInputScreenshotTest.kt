package com.sasayaki.screenshots

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sasayaki.data.preferences.BubbleSize
import com.sasayaki.service.bubble.BubbleActions
import com.sasayaki.service.bubble.BubbleAppearance
import com.sasayaki.service.bubble.BubblePhase
import com.sasayaki.service.bubble.BubbleUiState
import com.sasayaki.service.voice.VoiceInputPanel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = PHONE, application = Application::class)
class VoiceInputScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val actions = BubbleActions({}, {}, {}, {}, {}, {}, {})
    private val levels = List(18) { (it % 6) / 6f }

    @Test
    fun panels() = compose.screenshot("voice_input_panels") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                BubbleUiState(BubblePhase.Recording(paused = false), elapsedSeconds = 4, profileName = "Chat"),
                BubbleUiState(BubblePhase.Processing(cleaningUp = true)),
                BubbleUiState(BubblePhase.Failed("Dictation is off in password and banking fields.", canRetry = false)),
                BubbleUiState(BubblePhase.Idle)
            ).forEach { state ->
                VoiceInputPanel(state, { levels }, actions, BubbleAppearance(), BubbleSize.LARGER.scale, onSwitchKeyboard = {})
            }
        }
    }
}

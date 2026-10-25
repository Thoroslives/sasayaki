package com.sasayaki.service.voice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sasayaki.service.bubble.BubbleActions
import com.sasayaki.service.bubble.BubbleAppearance
import com.sasayaki.service.bubble.BubbleContent
import com.sasayaki.service.bubble.BubblePhase
import com.sasayaki.service.bubble.BubbleScales
import com.sasayaki.service.bubble.BubbleTheme
import com.sasayaki.service.bubble.BubbleUiState
import com.sasayaki.service.bubble.mode
import com.sasayaki.ui.theme.SasayakiIcons
import com.sasayaki.ui.theme.Spacing

// PanelHeight fits the largest controls, so the keyboard area never changes height mid-dictation.
private val PanelHeight = 132.dp

@Composable
fun VoiceInputPanel(
    state: BubbleUiState,
    levels: () -> List<Float>,
    actions: BubbleActions,
    appearance: BubbleAppearance,
    controlsScale: Float,
    onSwitchKeyboard: () -> Unit
) {
    // The idle bubble only appears here as a tap-to-retry button, so it matches the controls.
    val scales = BubbleScales(bubble = controlsScale, controls = controlsScale)
    BubbleTheme(appearance) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(PanelHeight)
                    .padding(horizontal = Spacing.s)
            ) {
                IconButton(onClick = onSwitchKeyboard, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(SasayakiIcons.Keyboard, contentDescription = "Back to keyboard")
                }
                if (state.phase == BubblePhase.Idle && !state.justInserted) {
                    Text(
                        text = "Tap to dictate",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = Spacing.s)
                    )
                }
                val mode = state.phase.mode
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(mode.windowWidth * controlsScale, mode.windowHeight * controlsScale)
                ) {
                    BubbleContent(state, levels, actions, onShowProfiles = {}, alignment = Alignment.Center, scales = scales)
                }
            }
        }
    }
}

package com.sasayaki.service.voice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
    BubbleTheme(appearance) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(PanelHeight)
                    .padding(horizontal = Spacing.s)
            ) {
                IconButton(onClick = onSwitchKeyboard) {
                    Icon(SasayakiIcons.Keyboard, contentDescription = "Back to keyboard")
                }
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val mode = state.phase.mode
                    // Wide pills shrink to fit beside the keyboard button instead of covering it.
                    val scale = controlsScale.coerceAtMost(maxWidth / mode.windowWidth)
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(mode.windowWidth * scale, mode.windowHeight * scale)
                    ) {
                        BubbleContent(
                            state = state,
                            levels = levels,
                            actions = actions,
                            onShowProfiles = {},
                            alignment = Alignment.Center,
                            scales = BubbleScales(bubble = scale, controls = scale)
                        )
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
                                .padding(bottom = Spacing.xs)
                        )
                    }
                }
                // Balances the keyboard button so the controls sit in the middle of the panel.
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

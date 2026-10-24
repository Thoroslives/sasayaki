package com.sasayaki.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.sasayaki.service.bubble.BubbleActions
import com.sasayaki.service.bubble.BubbleAppearance
import com.sasayaki.service.bubble.BubbleContent
import com.sasayaki.service.bubble.BubbleMode
import com.sasayaki.service.bubble.BubblePhase
import com.sasayaki.service.bubble.BubbleTheme
import com.sasayaki.service.bubble.BubbleUiState
import com.sasayaki.service.bubble.WAVEFORM_BARS
import com.sasayaki.service.bubble.mode
import com.sasayaki.ui.theme.Spacing
import kotlin.math.abs
import kotlin.math.sin

private const val BACKDROP =
    "Hey! Are we still on for Saturday? I booked the table for eight, and Mika said she might " +
        "bring her brother along. Let me know if the time works, or if we should move it a bit earlier."

private val previewActions = BubbleActions({}, {}, {}, {}, {}, {}, {})
private val previewLevels = List(WAVEFORM_BARS) { abs(sin(it * 0.7f)) * (0.3f + (it % 5) * 0.15f) }
private val recording = BubbleUiState(BubblePhase.Recording(paused = false), elapsedSeconds = 7, profileName = "Chat")
private val idle = BubbleUiState(BubblePhase.Idle)

// BubblePreview lays the bubble over sample text so see-through has something to show.
@Composable
fun BubblePreview(shapes: ListItemShapes, appearance: BubbleAppearance, scale: Float) {
    Surface(
        shape = shapes.shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {}
    ) {
        BoxWithConstraints(modifier = Modifier.padding(Spacing.l)) {
            Text(BACKDROP, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val fitted = scale.coerceAtMost(maxWidth / BubbleMode.RECORDING.windowWidth)
            BubbleTheme(appearance) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    PreviewWindow(recording, fitted)
                    PreviewWindow(idle, fitted)
                }
            }
        }
    }
}

@Composable
private fun PreviewWindow(state: BubbleUiState, scale: Float) {
    val mode = state.phase.mode
    Box(modifier = Modifier.size(mode.windowWidth * scale, mode.windowHeight * scale)) {
        BubbleContent(state, { previewLevels }, previewActions, {}, scale = scale)
    }
}

package com.sasayaki.service.bubble

import androidx.compose.runtime.Immutable
import com.sasayaki.service.ServiceState

sealed interface BubblePhase {
    data object Idle : BubblePhase
    data class Recording(val paused: Boolean) : BubblePhase
    data class Processing(val cleaningUp: Boolean) : BubblePhase
    data class Failed(val message: String, val canRetry: Boolean) : BubblePhase
}

@Immutable
data class BubbleUiState(
    val phase: BubblePhase = BubblePhase.Idle,
    val elapsedSeconds: Long = 0,
    val profileName: String = "",
    val justInserted: Boolean = false // briefly true after text actually reached the field
)

fun ServiceState.toPhase(): BubblePhase = when (this) {
    ServiceState.Idle, ServiceState.Injecting -> BubblePhase.Idle
    is ServiceState.Recording -> BubblePhase.Recording(paused)
    ServiceState.Transcribing -> BubblePhase.Processing(cleaningUp = false)
    ServiceState.PostProcessing -> BubblePhase.Processing(cleaningUp = true)
    is ServiceState.Error -> BubblePhase.Failed(message, canRetry = retryEntryId != null)
}

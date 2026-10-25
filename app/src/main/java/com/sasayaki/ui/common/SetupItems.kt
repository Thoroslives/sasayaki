package com.sasayaki.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.sasayaki.ui.theme.SasayakiIcons

const val ACCESSIBILITY_KEY = "accessibility"
private const val OVERLAY_KEY = "overlay"

// BUBBLE_KEYS are the steps only the bubble needs; the keyboard mic key works without them.
val BUBBLE_KEYS = setOf(OVERLAY_KEY, ACCESSIBILITY_KEY)

@Immutable
data class SetupItem(
    val key: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val done: Boolean,
    val required: Boolean,
    val actionLabel: String,
    val onAction: () -> Unit
)

@Composable
fun rememberPermissionItems(bubbleNeeded: Boolean = true): List<SetupItem> {
    val microphone = rememberMicrophonePermissionState()
    val overlay = rememberOverlayPermissionState()
    val accessibility = rememberAccessibilityPermissionState()
    val notifications = rememberNotificationPermissionState()
    return listOf(
        microphone.toItem("microphone", SasayakiIcons.Mic, required = true),
        overlay.toItem(OVERLAY_KEY, SasayakiIcons.Layers, required = bubbleNeeded),
        accessibility.toItem(ACCESSIBILITY_KEY, SasayakiIcons.Keyboard, required = bubbleNeeded),
        notifications.toItem("notifications", SasayakiIcons.Notifications, required = false)
    )
}

private fun PermissionStatus.toItem(key: String, icon: ImageVector, required: Boolean) = SetupItem(
    key = key,
    title = name,
    description = description,
    icon = icon,
    done = granted,
    required = required,
    actionLabel = actionLabel,
    onAction = onRequest
)

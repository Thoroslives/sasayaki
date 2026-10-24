package com.sasayaki.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.sasayaki.ui.theme.SasayakiIcons

const val ACCESSIBILITY_KEY = "accessibility"

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
fun rememberPermissionItems(): List<SetupItem> {
    val microphone = rememberMicrophonePermissionState()
    val overlay = rememberOverlayPermissionState()
    val accessibility = rememberAccessibilityPermissionState()
    val notifications = rememberNotificationPermissionState()
    return listOf(
        microphone.toItem("microphone", SasayakiIcons.Mic, required = true),
        overlay.toItem("overlay", SasayakiIcons.Layers, required = true),
        accessibility.toItem(ACCESSIBILITY_KEY, SasayakiIcons.Keyboard, required = true),
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

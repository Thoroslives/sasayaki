package com.sasayaki.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sasayaki.data.preferences.BubbleSize
import com.sasayaki.data.preferences.ThemeMode
import com.sasayaki.ui.common.rememberPermissionItems
import com.sasayaki.ui.common.rememberRestrictedSettingsLikely
import com.sasayaki.ui.components.ConnectedChoice
import com.sasayaki.ui.components.ItemIcon
import com.sasayaki.ui.components.NavigationItem
import com.sasayaki.ui.components.SegmentedGroup
import com.sasayaki.ui.components.SegmentedItem
import com.sasayaki.ui.components.SettingsPage
import com.sasayaki.ui.components.SetupGroup
import com.sasayaki.ui.components.SwitchItem
import com.sasayaki.ui.components.pageIntro
import com.sasayaki.ui.components.segmentedItemColors
import com.sasayaki.ui.theme.SasayakiIcons
import com.sasayaki.ui.theme.Spacing
import java.util.Locale

private val retentionOptions = listOf(50, 100, 500, 1000, 5000)

@Composable
fun RecordingSettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    SettingsPage(title = "Recording", onBack = onBack) {
        if (!state.loaded) return@SettingsPage
        item(key = "listening") {
            SegmentedGroup(
                title = "Listening",
                items = listOf(
                    { shapes -> SilenceItem(shapes, prefs.silenceThresholdMs, viewModel::setSilenceThreshold) },
                    { shapes ->
                        SwitchItem(
                            shapes = shapes,
                            headline = "Pause other audio",
                            supporting = "Music and videos pause while you speak",
                            icon = SasayakiIcons.MusicOff,
                            checked = prefs.pauseOtherAudio,
                            onCheckedChange = viewModel::setPauseOtherAudio
                        )
                    },
                    { shapes ->
                        SwitchItem(
                            shapes = shapes,
                            headline = "Haptic feedback",
                            supporting = "A short buzz when recording starts and stops",
                            icon = SasayakiIcons.Vibration,
                            checked = prefs.vibrateOnRecord,
                            onCheckedChange = viewModel::setVibrateOnRecord
                        )
                    }
                )
            )
        }
        item(key = "delivery") {
            SegmentedGroup(
                title = "Delivery",
                items = listOf(
                    { shapes ->
                        SwitchItem(
                            shapes = shapes,
                            headline = "Copy when typing fails",
                            supporting = "If the text can't be typed into the field, it is copied instead",
                            icon = SasayakiIcons.ContentPaste,
                            checked = prefs.autoClipboard,
                            onCheckedChange = viewModel::setAutoClipboard
                        )
                    },
                    { shapes ->
                        SwitchItem(
                            shapes = shapes,
                            headline = "Start on boot",
                            supporting = "Turn the bubble back on after a restart",
                            icon = SasayakiIcons.PowerSettings,
                            checked = prefs.startOnBoot,
                            onCheckedChange = viewModel::setStartOnBoot
                        )
                    }
                )
            )
        }
    }
}

@Composable
fun HistorySettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    SettingsPage(title = "History and privacy", onBack = onBack) {
        if (!state.loaded) return@SettingsPage
        pageIntro("Dictations never leave this device except to reach your providers.")
        item(key = "history") {
            SegmentedGroup(
                items = listOf(
                    { shapes ->
                        SwitchItem(
                            shapes = shapes,
                            headline = "Keep history",
                            supporting = "Saves text and audio so you can copy or retry later",
                            icon = SasayakiIcons.History,
                            checked = prefs.historyEnabled,
                            onCheckedChange = viewModel::setHistoryEnabled
                        )
                    },
                    { shapes ->
                        RetentionItem(shapes, prefs.historyRetentionLimit, prefs.historyEnabled, viewModel::setHistoryRetention)
                    },
                    { shapes ->
                        SwitchItem(
                            shapes = shapes,
                            headline = "Count words without history",
                            supporting = "Keep your totals when nothing else is saved",
                            icon = SasayakiIcons.Speed,
                            checked = prefs.keepStatsWithoutHistory,
                            onCheckedChange = viewModel::setKeepStatsWithoutHistory,
                            enabled = !prefs.historyEnabled
                        )
                    }
                )
            )
        }
    }
}

@Composable
fun AppearanceSettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    SettingsPage(title = "Appearance", onBack = onBack) {
        if (!state.loaded) return@SettingsPage
        item(key = "appearance") {
            val items = buildList<SegmentedItem> {
                add { shapes -> ThemeItem(shapes, prefs.themeMode, viewModel::setThemeMode) }
                add { shapes -> BubbleSizeItem(shapes, prefs.bubbleSize, viewModel::setBubbleSize) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add { shapes ->
                        SwitchItem(
                            shapes = shapes,
                            headline = "Wallpaper colors",
                            supporting = "Match your wallpaper instead of Sasayaki green",
                            icon = SasayakiIcons.Wallpaper,
                            checked = prefs.wallpaperColors,
                            onCheckedChange = viewModel::setWallpaperColors
                        )
                    }
                }
            }
            SegmentedGroup(items = items)
        }
    }
}

@Composable
fun PermissionsRoute(onBack: () -> Unit) {
    val items = rememberPermissionItems()
    val restricted = rememberRestrictedSettingsLikely()
    SettingsPage(title = "Permissions", onBack = onBack) {
        pageIntro("Sasayaki needs these to listen when you ask and type the result where you are.")
        item(key = "permissions") {
            SetupGroup(items = items, restrictedSettingsLikely = restricted)
        }
    }
}

@Composable
private fun SilenceItem(shapes: ListItemShapes, thresholdMs: Long, onChange: (Long) -> Unit) {
    val state = rememberSliderState(value = thresholdMs / 1000f, steps = 8, trackRange = 0.5f..5f)
    SegmentedListItem(
        shapes = shapes,
        colors = segmentedItemColors(),
        leadingContent = { ItemIcon(SasayakiIcons.Timer) },
        trailingContent = {
            Text(String.format(Locale.getDefault(), "%.1f s", state.value), style = MaterialTheme.typography.labelLarge)
        },
        supportingContent = {
            Column {
                Text("Recording stops after this much quiet")
                Slider(
                    state = state,
                    onValueChange = { state.value = it },
                    onValueChangeFinished = { onChange((state.value * 1000).toLong()) },
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }
        }
    ) {
        Text("Stop after silence")
    }
}

@Composable
private fun RetentionItem(shapes: ListItemShapes, limit: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        NavigationItem(
            shapes = shapes,
            headline = "Keep the last",
            supporting = "Older dictations are removed automatically",
            icon = SasayakiIcons.DeleteSweep,
            trailingText = "$limit",
            enabled = enabled,
            onClick = { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            retentionOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text("$option dictations") },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    trailingIcon = if (option == limit) {
                        { Icon(SasayakiIcons.Check, contentDescription = null) }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@Composable
private fun BubbleSizeItem(shapes: ListItemShapes, size: BubbleSize, onSelect: (BubbleSize) -> Unit) {
    SegmentedListItem(
        shapes = shapes,
        colors = segmentedItemColors(),
        leadingContent = { ItemIcon(SasayakiIcons.ChatBubble) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.padding(top = Spacing.xs)) {
                Text("Bigger bubbles have bigger buttons, easier to hit with gloves or on the move.")
                ConnectedChoice(
                    options = BubbleSize.entries,
                    selected = size,
                    onSelect = onSelect,
                    label = {
                        when (it) {
                            BubbleSize.STANDARD -> "Standard"
                            BubbleSize.LARGE -> "Large"
                            BubbleSize.LARGER -> "Larger"
                        }
                    }
                )
            }
        }
    ) {
        Text("Bubble size")
    }
}

@Composable
private fun ThemeItem(shapes: ListItemShapes, mode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    SegmentedListItem(
        shapes = shapes,
        colors = segmentedItemColors(),
        leadingContent = { ItemIcon(SasayakiIcons.Palette) },
        supportingContent = {
            ConnectedChoice(
                options = ThemeMode.entries,
                selected = mode,
                onSelect = onSelect,
                label = {
                    when (it) {
                        ThemeMode.SYSTEM -> "System"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    }
                },
                modifier = Modifier.padding(top = Spacing.s)
            )
        }
    ) {
        Text("Theme")
    }
}

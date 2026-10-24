package com.sasayaki.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sasayaki.BuildConfig
import com.sasayaki.data.preferences.ThemeMode
import com.sasayaki.ui.common.rememberPermissionItems
import com.sasayaki.ui.components.ItemIcon
import com.sasayaki.ui.components.NavigationItem
import com.sasayaki.ui.components.SegmentedGroup
import com.sasayaki.ui.components.SettingsPage
import com.sasayaki.ui.components.segmentedItemColors
import com.sasayaki.ui.theme.SasayakiIcons
import java.util.Locale

private const val SOURCE_URL = "https://github.com/pluja/sasayaki"

class SettingsDestinations(
    val onProviders: () -> Unit,
    val onRecording: () -> Unit,
    val onRules: () -> Unit,
    val onPrompts: () -> Unit,
    val onPermissions: () -> Unit,
    val onHistory: () -> Unit,
    val onAppearance: () -> Unit
)

@Composable
fun SettingsRoute(destinations: SettingsDestinations, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val missingPermissions = rememberPermissionItems().count { it.required && !it.done }
    SettingsScreen(
        state = state,
        missingPermissions = missingPermissions,
        destinations = destinations,
        onOpenSource = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL))) }
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    missingPermissions: Int,
    destinations: SettingsDestinations,
    onOpenSource: () -> Unit
) {
    val prefs = state.preferences
    SettingsPage(title = "Settings", onBack = null) {
        if (!state.loaded) return@SettingsPage
        item(key = "dictation") {
            SegmentedGroup(
                title = "Dictation",
                items = listOf(
                    { shapes ->
                        NavigationItem(
                            shapes = shapes,
                            headline = "Providers",
                            supporting = state.providers.joinToString { it.name }.ifEmpty { "None yet. Add one to start dictating." },
                            needsAttention = state.providers.isEmpty(),
                            icon = SasayakiIcons.Dns,
                            onClick = destinations.onProviders
                        )
                    },
                    { shapes ->
                        NavigationItem(
                            shapes = shapes,
                            headline = "Recording",
                            supporting = "Stops after ${String.format(Locale.getDefault(), "%.1f", prefs.silenceThresholdMs / 1000f)} s of silence",
                            icon = SasayakiIcons.Mic,
                            onClick = destinations.onRecording
                        )
                    },
                    { shapes ->
                        NavigationItem(
                            shapes = shapes,
                            headline = "Replacement rules",
                            supporting = when (state.ruleCount) {
                                0 -> "Fix words your speech model gets wrong"
                                1 -> "1 rule"
                                else -> "${state.ruleCount} rules"
                            },
                            icon = SasayakiIcons.FindReplace,
                            onClick = destinations.onRules
                        )
                    },
                    { shapes ->
                        NavigationItem(
                            shapes = shapes,
                            headline = "Prompts",
                            supporting = when (state.customPromptCount) {
                                0 -> "Extra instructions for cleanup"
                                1 -> "1 of your own"
                                else -> "${state.customPromptCount} of your own"
                            },
                            icon = SasayakiIcons.Notes,
                            onClick = destinations.onPrompts
                        )
                    }
                )
            )
        }
        item(key = "app") {
            SegmentedGroup(
                title = "App",
                items = listOf(
                    { shapes ->
                        NavigationItem(
                            shapes = shapes,
                            headline = "Permissions",
                            supporting = when (missingPermissions) {
                                0 -> "All set"
                                1 -> "1 needs your attention"
                                else -> "$missingPermissions need your attention"
                            },
                            needsAttention = missingPermissions > 0,
                            icon = SasayakiIcons.Shield,
                            onClick = destinations.onPermissions
                        )
                    },
                    { shapes ->
                        NavigationItem(
                            shapes = shapes,
                            headline = "History and privacy",
                            supporting = if (prefs.historyEnabled) "Keeping the last ${prefs.historyRetentionLimit}" else "Not keeping history",
                            icon = SasayakiIcons.History,
                            onClick = destinations.onHistory
                        )
                    },
                    { shapes ->
                        NavigationItem(
                            shapes = shapes,
                            headline = "Appearance",
                            supporting = listOfNotNull(
                                prefs.themeMode.label(),
                                "wallpaper colors".takeIf { prefs.wallpaperColors }
                            ).joinToString(" · "),
                            icon = SasayakiIcons.Palette,
                            onClick = destinations.onAppearance
                        )
                    }
                )
            )
        }
        item(key = "about") {
            SegmentedGroup(
                title = "About",
                items = listOf(
                    { shapes ->
                        NavigationItem(
                            shapes = shapes,
                            headline = "Source code",
                            supporting = "Free and open source",
                            icon = SasayakiIcons.Code,
                            onClick = onOpenSource
                        )
                    },
                    { shapes ->
                        SegmentedListItem(
                            shapes = shapes,
                            colors = segmentedItemColors(),
                            leadingContent = { ItemIcon(SasayakiIcons.Info) },
                            trailingContent = { Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.labelLarge) }
                        ) {
                            Text("Version")
                        }
                    }
                )
            )
        }
    }
}

fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System theme"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

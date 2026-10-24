package com.sasayaki.service.bubble

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.sasayaki.ui.theme.Graphite10
import com.sasayaki.ui.theme.Graphite17
import com.sasayaki.ui.theme.Graphite24
import com.sasayaki.ui.theme.Graphite30
import com.sasayaki.ui.theme.Graphite80
import com.sasayaki.ui.theme.Graphite90
import com.sasayaki.ui.theme.SasayakiTheme
import com.sasayaki.ui.theme.sasayakiColorScheme

data class BubbleAppearance(
    val wallpaperColors: Boolean = false,
    val translucent: Boolean = false,
    val monochrome: Boolean = false
)

internal val LocalBubbleTranslucent = staticCompositionLocalOf { false }

/**
 * BubbleTheme is always dark: the bubble floats over apps of either theme, and one look
 * keeps it recognisable everywhere. Monochrome keeps error colors so a failure still
 * stands out.
 */
@Composable
fun BubbleTheme(appearance: BubbleAppearance, content: @Composable () -> Unit) {
    val base = sasayakiColorScheme(darkTheme = true, wallpaperColors = appearance.wallpaperColors)
    SasayakiTheme(darkTheme = true, colorScheme = if (appearance.monochrome) base.monochrome() else base) {
        CompositionLocalProvider(LocalBubbleTranslucent provides appearance.translucent, content = content)
    }
}

private fun ColorScheme.monochrome() = copy(
    primary = Graphite80,
    onPrimary = Graphite10,
    primaryContainer = Graphite24,
    onPrimaryContainer = Graphite90,
    tertiary = Graphite90,
    onTertiary = Graphite10,
    surfaceContainerHigh = Graphite17,
    surfaceContainerHighest = Graphite24,
    onSurface = Graphite90,
    onSurfaceVariant = Graphite80,
    outlineVariant = Graphite30
)

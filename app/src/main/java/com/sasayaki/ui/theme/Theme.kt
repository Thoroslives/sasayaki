package com.sasayaki.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Pine40,
    onPrimary = Color.White,
    primaryContainer = Pine90,
    onPrimaryContainer = Pine30,
    inversePrimary = Pine80,
    secondary = Sage40,
    onSecondary = Color.White,
    secondaryContainer = Sage90,
    onSecondaryContainer = Sage30,
    tertiary = Clay40,
    onTertiary = Color.White,
    tertiaryContainer = Clay90,
    onTertiaryContainer = Clay30,
    error = Ember40,
    onError = Color.White,
    errorContainer = Ember90,
    onErrorContainer = Ember30,
    background = Ink98,
    onBackground = Ink10,
    surface = Ink98,
    onSurface = Ink10,
    surfaceVariant = Mist90,
    onSurfaceVariant = Mist30,
    surfaceTint = Pine40,
    inverseSurface = Ink20,
    inverseOnSurface = Ink95,
    outline = Mist50,
    outlineVariant = Mist80,
    scrim = Color.Black,
    surfaceBright = Ink98,
    surfaceDim = Ink87,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Ink96,
    surfaceContainer = Ink94,
    surfaceContainerHigh = Ink92,
    surfaceContainerHighest = Ink90,
    primaryFixed = Pine90,
    primaryFixedDim = Pine80,
    onPrimaryFixed = Pine10,
    onPrimaryFixedVariant = Pine30,
    secondaryFixed = Sage90,
    secondaryFixedDim = Sage80,
    onSecondaryFixed = Sage10,
    onSecondaryFixedVariant = Sage30,
    tertiaryFixed = Clay90,
    tertiaryFixedDim = Clay80,
    onTertiaryFixed = Clay10,
    onTertiaryFixedVariant = Clay30
)

private val DarkColors = darkColorScheme(
    primary = Pine80,
    onPrimary = Pine20,
    primaryContainer = Pine30,
    onPrimaryContainer = Pine90,
    inversePrimary = Pine40,
    secondary = Sage80,
    onSecondary = Sage20,
    secondaryContainer = Sage30,
    onSecondaryContainer = Sage90,
    tertiary = Clay80,
    onTertiary = Clay20,
    tertiaryContainer = Clay30,
    onTertiaryContainer = Clay90,
    error = Ember80,
    onError = Ember20,
    errorContainer = Ember30,
    onErrorContainer = Ember90,
    background = Ink6,
    onBackground = Ink90,
    surface = Ink6,
    onSurface = Ink90,
    surfaceVariant = Mist30,
    onSurfaceVariant = Mist80,
    surfaceTint = Pine80,
    inverseSurface = Ink90,
    inverseOnSurface = Ink20,
    outline = Mist60,
    outlineVariant = Mist30,
    scrim = Color.Black,
    surfaceBright = Ink24,
    surfaceDim = Ink6,
    surfaceContainerLowest = Ink4,
    surfaceContainerLow = Ink10,
    surfaceContainer = Ink12,
    surfaceContainerHigh = Ink17,
    surfaceContainerHighest = Ink22,
    primaryFixed = Pine90,
    primaryFixedDim = Pine80,
    onPrimaryFixed = Pine10,
    onPrimaryFixedVariant = Pine30,
    secondaryFixed = Sage90,
    secondaryFixedDim = Sage80,
    onSecondaryFixed = Sage10,
    onSecondaryFixedVariant = Sage30,
    tertiaryFixed = Clay90,
    tertiaryFixedDim = Clay80,
    onTertiaryFixed = Clay10,
    onTertiaryFixedVariant = Clay30
)

@Composable
fun sasayakiColorScheme(darkTheme: Boolean, wallpaperColors: Boolean): ColorScheme {
    if (wallpaperColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    return if (darkTheme) DarkColors else LightColors
}

@Composable
fun SasayakiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    wallpaperColors: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = sasayakiColorScheme(darkTheme, wallpaperColors),
        motionScheme = MotionScheme.expressive(),
        shapes = SasayakiShapes,
        typography = SasayakiTypography,
        content = content
    )
}

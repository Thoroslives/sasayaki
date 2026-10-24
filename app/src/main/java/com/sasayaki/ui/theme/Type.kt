package com.sasayaki.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System families only: bundling or downloading fonts would cost APK size or a Play
// Services dependency, and the serif/sans pairing carries the brand on its own.
private val Editorial = FontFamily.Serif
private val Utility = FontFamily.SansSerif

private fun style(family: FontFamily, weight: FontWeight, size: Int, lineHeight: Int, tracking: Double) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp
)

private val displayLarge = style(Editorial, FontWeight.SemiBold, 56, 62, -0.5)
private val displayMedium = style(Editorial, FontWeight.SemiBold, 44, 52, -0.25)
private val displaySmall = style(Editorial, FontWeight.SemiBold, 36, 44, -0.2)
private val headlineLarge = style(Editorial, FontWeight.SemiBold, 32, 40, -0.2)
private val headlineMedium = style(Editorial, FontWeight.SemiBold, 28, 36, 0.0)
private val headlineSmall = style(Editorial, FontWeight.SemiBold, 24, 32, 0.0)
private val titleLarge = style(Editorial, FontWeight.Medium, 22, 28, 0.0)
private val titleMedium = style(Utility, FontWeight.SemiBold, 16, 24, 0.1)
private val titleSmall = style(Utility, FontWeight.SemiBold, 14, 20, 0.1)
private val bodyLarge = style(Utility, FontWeight.Normal, 16, 24, 0.15)
private val bodyMedium = style(Utility, FontWeight.Normal, 14, 20, 0.2)
private val bodySmall = style(Utility, FontWeight.Normal, 12, 16, 0.3)
private val labelLarge = style(Utility, FontWeight.Medium, 14, 20, 0.1)
private val labelMedium = style(Utility, FontWeight.Medium, 12, 16, 0.4)
private val labelSmall = style(Utility, FontWeight.Medium, 11, 16, 0.4)

val SasayakiTypography = Typography(
    displayLarge = displayLarge,
    displayMedium = displayMedium,
    displaySmall = displaySmall,
    headlineLarge = headlineLarge,
    headlineMedium = headlineMedium,
    headlineSmall = headlineSmall,
    titleLarge = titleLarge,
    titleMedium = titleMedium,
    titleSmall = titleSmall,
    bodyLarge = bodyLarge,
    bodyMedium = bodyMedium,
    bodySmall = bodySmall,
    labelLarge = labelLarge,
    labelMedium = labelMedium,
    labelSmall = labelSmall,
    displayLargeEmphasized = displayLarge.copy(fontWeight = FontWeight.Bold),
    displayMediumEmphasized = displayMedium.copy(fontWeight = FontWeight.Bold),
    displaySmallEmphasized = displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineLargeEmphasized = headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMediumEmphasized = headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmallEmphasized = headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLargeEmphasized = titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMediumEmphasized = titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmallEmphasized = titleSmall.copy(fontWeight = FontWeight.Bold),
    bodyLargeEmphasized = bodyLarge.copy(fontWeight = FontWeight.Medium),
    bodyMediumEmphasized = bodyMedium.copy(fontWeight = FontWeight.Medium),
    bodySmallEmphasized = bodySmall.copy(fontWeight = FontWeight.Medium),
    labelLargeEmphasized = labelLarge.copy(fontWeight = FontWeight.Bold),
    labelMediumEmphasized = labelMedium.copy(fontWeight = FontWeight.Bold),
    labelSmallEmphasized = labelSmall.copy(fontWeight = FontWeight.Bold)
)

package com.sasayaki.ui.profiles

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import com.sasayaki.domain.model.OutputStyle
import com.sasayaki.domain.model.RewriteMode
import com.sasayaki.domain.model.SummarizeMode
import com.sasayaki.domain.processing.applyPunctuationStyle
import com.sasayaki.ui.theme.Spacing

const val SAMPLE_DICTATION =
    "um so I was thinking, like, we could grab dinner on Friday, no wait, Saturday, at that new Thai " +
        "place, you know, the one by the park, the one that just opened, if you're free"

private const val SAMPLE_EMOJI = " 🍜"

// sampleTexts are written by hand per rewrite and shorten level, in full punctuation,
// to show what each level asks the model for; punctuation styles are derived from them.
private val sampleTexts: Map<Pair<RewriteMode, SummarizeMode>, String> = mapOf(
    (RewriteMode.NONE to SummarizeMode.NONE) to
        "So I was thinking we could grab dinner on Saturday at that new Thai place, the one by the park, " +
        "the one that just opened, if you're free.",
    (RewriteMode.NONE to SummarizeMode.LIGHT) to
        "I was thinking we could grab dinner on Saturday at that new Thai place, the one by the park, if you're free.",
    (RewriteMode.NONE to SummarizeMode.HARD) to "Dinner on Saturday at that new Thai place, if you're free.",
    (RewriteMode.FIX to SummarizeMode.NONE) to
        "I was thinking we could grab dinner on Saturday at the new Thai place by the park, the one that " +
        "just opened, if you're free.",
    (RewriteMode.FIX to SummarizeMode.LIGHT) to
        "I was thinking we could grab dinner on Saturday at the new Thai place by the park, if you're free.",
    (RewriteMode.FIX to SummarizeMode.HARD) to "Dinner on Saturday at the new Thai place by the park?",
    (RewriteMode.POLISH to SummarizeMode.NONE) to
        "Would you like to have dinner on Saturday? I was thinking of the new Thai restaurant by the park, " +
        "which just opened.",
    (RewriteMode.POLISH to SummarizeMode.LIGHT) to
        "Would you like to have dinner on Saturday at the new Thai restaurant by the park?",
    (RewriteMode.POLISH to SummarizeMode.HARD) to "Dinner on Saturday at the new Thai restaurant?"
)

fun styleSample(style: OutputStyle, rewrite: RewriteMode, summarize: SummarizeMode, emoji: Boolean): String {
    val text = sampleTexts.getValue(rewrite to summarize)
    val punctuated = applyPunctuationStyle(text, style)
    return if (emoji && rewrite != RewriteMode.POLISH) punctuated + SAMPLE_EMOJI else punctuated
}

@Composable
fun StylePreview(
    shapes: ListItemShapes,
    style: OutputStyle,
    rewrite: RewriteMode,
    summarize: SummarizeMode,
    emoji: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    Surface(shape = shapes.shape, color = colors.secondaryContainer, modifier = modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m)
        ) {
            Text("You say", style = MaterialTheme.typography.labelMedium, color = colors.onSecondaryContainer)
            Text(
                SAMPLE_DICTATION,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = colors.onSecondaryContainer.copy(alpha = 0.72f)
            )
            Text(
                "You get",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSecondaryContainer,
                modifier = Modifier.padding(top = Spacing.s)
            )
            AnimatedContent(
                targetState = styleSample(style, rewrite, summarize, emoji),
                transitionSpec = { fadeIn(fadeInSpec) togetherWith fadeOut(fadeOutSpec) },
                label = "style sample"
            ) { sample ->
                Text(sample, style = MaterialTheme.typography.bodyLarge, color = colors.onSecondaryContainer)
            }
        }
    }
}

package com.sasayaki.domain.processing

import com.sasayaki.domain.model.OutputStyle

/**
 * applyPunctuationStyle derives the lighter styles from fully punctuated text, mirroring
 * what [SystemPromptBuilder.casingRule] asks the model for.
 */
fun applyPunctuationStyle(text: String, style: OutputStyle): String = when (style) {
    OutputStyle.STANDARD -> text
    OutputStyle.RELAXED -> text.replace(",", "").removeSuffix(".")
    OutputStyle.MINIMAL -> text.lowercase().filter { it.isLetterOrDigit() || it.isWhitespace() || it == '\'' }
}

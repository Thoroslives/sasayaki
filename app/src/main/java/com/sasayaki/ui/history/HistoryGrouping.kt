package com.sasayaki.ui.history

import androidx.compose.runtime.Immutable
import com.sasayaki.data.db.entity.DictationSummary
import com.sasayaki.ui.common.displaySourceApp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Immutable
data class DayGroup(
    val key: String,
    val label: String,
    val totalWords: Int,
    val dictations: List<DictationSummary>
)

private val dayFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

fun groupByDay(
    dictations: List<DictationSummary>,
    zone: ZoneId,
    today: LocalDate,
    query: String = "",
    hiddenIds: Set<Long> = emptySet()
): List<DayGroup> {
    val needle = query.trim()
    return dictations
        .asSequence()
        .filter { it.id !in hiddenIds }
        .filter { needle.isEmpty() || it.matches(needle) }
        .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        .entries
        .sortedByDescending { it.key }
        .map { (date, items) ->
            DayGroup(
                key = date.toString(),
                label = dayLabel(date, today),
                totalWords = items.sumOf { it.wordCount },
                dictations = items.sortedByDescending { it.timestamp }
            )
        }
}

private fun DictationSummary.matches(needle: String): Boolean =
    text.contains(needle, ignoreCase = true) ||
        displaySourceApp(sourceApp, sourceAppPackage)?.contains(needle, ignoreCase = true) == true

private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> dayFormatter.format(date)
}

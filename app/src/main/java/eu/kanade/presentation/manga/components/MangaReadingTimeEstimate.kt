package eu.kanade.presentation.manga.components

import android.content.Context
import eu.kanade.presentation.util.toDurationString
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal fun Int.toEstimatedReadingTime(context: Context): String? {
    if (this <= 0) return null

    return (toLong() * AVERAGE_CHAPTER_READING_TIME_MILLIS)
        .toDuration(DurationUnit.MILLISECONDS)
        .toDurationString(context, fallback = "")
        .takeIf { it.isNotBlank() }
}

private const val AVERAGE_CHAPTER_READING_TIME_MILLIS = 182_800L

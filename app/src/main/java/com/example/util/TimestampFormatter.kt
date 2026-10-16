package com.example.util

import java.util.Locale

object TimestampFormatter {

    /**
     * Converts microseconds to HH:MM:SS.mmm string.
     * E.g. 5,250,000 us -> "00:00:05.250"
     */
    fun formatUsToHhMmSsMmm(timeUs: Long): String {
        val totalMs = (timeUs.coerceAtLeast(0L) / 1000L)
        val ms = (totalMs % 1000L).toInt()
        val totalSeconds = totalMs / 1000L
        val seconds = (totalSeconds % 60L).toInt()
        val totalMinutes = totalSeconds / 60L
        val minutes = (totalMinutes % 60L).toInt()
        val hours = (totalMinutes / 60L).toInt()

        return String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, ms)
    }

    /**
     * Converts microseconds to a filename-safe string.
     * E.g. 5,250,000 us -> "00-00-05-250"
     */
    fun formatUsToFilenameTimestamp(timeUs: Long): String {
        val totalMs = (timeUs.coerceAtLeast(0L) / 1000L)
        val ms = (totalMs % 1000L).toInt()
        val totalSeconds = totalMs / 1000L
        val seconds = (totalSeconds % 60L).toInt()
        val totalMinutes = totalSeconds / 60L
        val minutes = (totalMinutes % 60L).toInt()
        val hours = (totalMinutes / 60L).toInt()

        return String.format(Locale.US, "%02d-%02d-%02d-%03d", hours, minutes, seconds, ms)
    }

    /**
     * Parses HH:MM:SS.mmm or HH:MM:SS:mmm or MM:SS.mmm into microseconds.
     */
    fun parseTimestampToUs(input: String): Long? {
        val clean = input.trim()
        if (clean.isEmpty()) return null

        return try {
            val parts = clean.split(":")
            if (parts.size == 3) {
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                val secParts = parts[2].split(".")
                val seconds = secParts[0].toLong()
                val ms = if (secParts.size > 1) {
                    secParts[1].padEnd(3, '0').take(3).toLong()
                } else 0L

                val totalMs = (hours * 3600_000L) + (minutes * 60_000L) + (seconds * 1000L) + ms
                totalMs * 1000L
            } else if (parts.size == 2) {
                val minutes = parts[0].toLong()
                val secParts = parts[1].split(".")
                val seconds = secParts[0].toLong()
                val ms = if (secParts.size > 1) {
                    secParts[1].padEnd(3, '0').take(3).toLong()
                } else 0L

                val totalMs = (minutes * 60_000L) + (seconds * 1000L) + ms
                totalMs * 1000L
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Calculates the sequence of timestamps to extract without floating-point error accumulation.
     */
    fun generateTimestamps(startUs: Long, endUs: Long, intervalUs: Long, maxCount: Int): List<Long> {
        if (intervalUs <= 0 || endUs < startUs) return emptyList()
        val list = mutableListOf<Long>()
        var frameIndex = 0L
        while (true) {
            val ts = startUs + frameIndex * intervalUs
            if (ts > endUs) break
            list.add(ts)
            if (list.size >= maxCount) break
            frameIndex++
        }
        return list
    }

    fun calculateEstimatedFrameCount(startUs: Long, endUs: Long, intervalUs: Long): Int {
        if (intervalUs <= 0L || endUs < startUs) return 0
        val diff = endUs - startUs
        val count = (diff / intervalUs) + 1
        return count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun formatDurationMs(durationMs: Long): String {
        val totalSec = durationMs / 1000L
        val min = totalSec / 60L
        val sec = totalSec % 60L
        val ms = durationMs % 1000L
        return String.format(Locale.US, "%02d:%02d.%03d", min, sec, ms)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIdx = 0
        while (size >= 1024 && unitIdx < units.size - 1) {
            size /= 1024
            unitIdx++
        }
        return String.format(Locale.US, "%.1f %s", size, units[unitIdx])
    }
}

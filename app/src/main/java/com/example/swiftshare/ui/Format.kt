package com.example.swiftshare.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

/** Human-readable byte sizes. Binary units (1 MB = 1024 * 1024 bytes). */
fun formatBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0)
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        safe >= gb -> String.format(Locale.US, "%.2f GB", safe / gb)
        safe >= mb -> String.format(Locale.US, "%.2f MB", safe / mb)
        safe >= kb -> String.format(Locale.US, "%.1f KB", safe / kb)
        else -> "$safe B"
    }
}

/** "m:ss" remaining time, or an em dash when it cannot be estimated. */
fun formatEta(seconds: Long?): String {
    if (seconds == null) return "—"
    val safe = seconds.coerceAtLeast(0)
    val minutes = safe / 60
    val rest = safe % 60
    return String.format(Locale.US, "%d:%02d", minutes, rest)
}

/** MB/s with two decimals. */
fun formatSpeed(mbps: Double): String =
    String.format(Locale.US, "%.2f", if (mbps.isFinite() && mbps >= 0) mbps else 0.0)

/** Localised timestamp for the history list. */
fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return ""
    return try {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    } catch (_: Exception) {
        ""
    }
}

/** Percentage 0..100 for progress indicators, guarding against bad input. */
fun progressPercent(transferred: Long, total: Long): Float {
    if (total <= 0) return 0f
    return ((transferred.toDouble() / total.toDouble()) * 100.0)
        .coerceIn(0.0, 100.0)
        .roundToLong()
        .toFloat()
}

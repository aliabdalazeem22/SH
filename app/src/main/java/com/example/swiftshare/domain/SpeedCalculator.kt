package com.example.swiftshare.domain

/**
 * Calculates transfer speed and estimated time remaining safely.
 *
 * Speed = transferredBytes / elapsedSeconds. If elapsed is 0 or the transfer is
 * interrupted, speed is reported as 0 and ETA as null rather than a misleading value.
 */
object SpeedCalculator {

    fun calculate(
        transferredBytes: Long,
        totalBytes: Long,
        startedAt: Long,
        now: Long = System.currentTimeMillis()
    ): SpeedSnapshot {
        val elapsedMs = (now - startedAt).coerceAtLeast(0)
        val elapsedSeconds = elapsedMs / 1000.0
        val speedBytesPerSecond = if (elapsedSeconds > 0) transferredBytes.toDouble() / elapsedSeconds else 0.0
        val speedMbps = speedBytesPerSecond / (1024.0 * 1024.0)

        val remainingBytes = (totalBytes - transferredBytes).coerceAtLeast(0)
        val etaSeconds = if (speedBytesPerSecond > 0 && remainingBytes > 0) {
            (remainingBytes / speedBytesPerSecond).toLong()
        } else if (transferredBytes >= totalBytes && totalBytes > 0) {
            0L
        } else {
            null
        }

        return SpeedSnapshot(
            speedMbps = speedMbps,
            etaSeconds = etaSeconds,
            elapsedSeconds = elapsedSeconds.toLong().coerceAtLeast(0),
            transferredBytes = transferredBytes,
            totalBytes = totalBytes,
            progressPercent = if (totalBytes > 0) (transferredBytes.toDouble() / totalBytes * 100.0) else 0.0
        )
    }

    data class SpeedSnapshot(
        val speedMbps: Double,
        val etaSeconds: Long?,
        val elapsedSeconds: Long,
        val transferredBytes: Long,
        val totalBytes: Long,
        val progressPercent: Double
    )
}

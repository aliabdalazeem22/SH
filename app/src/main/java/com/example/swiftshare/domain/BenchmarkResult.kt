package com.example.swiftshare.domain

/**
 * Result of a real throughput benchmark. Values are measured, never hard-coded.
 */
data class BenchmarkResult(
    val testFileSizeBytes: Long,
    val elapsedMs: Long,
    val throughputMbps: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    val testFileSizeMB: Double get() = testFileSizeBytes / (1024.0 * 1024.0)
    val elapsedSeconds: Double get() = elapsedMs / 1000.0
}

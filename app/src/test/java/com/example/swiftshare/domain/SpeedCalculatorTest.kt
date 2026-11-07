package com.example.swiftshare.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedCalculatorTest {

    @Test
    fun `speed is bytes divided by elapsed seconds`() {
        val snapshot = SpeedCalculator.calculate(
            transferredBytes = 2L * 1024 * 1024,
            totalBytes = 4L * 1024 * 1024,
            startedAt = 1_000L,
            now = 3_000L
        )

        assertEquals(1.0, snapshot.speedMbps, 0.0001)
        assertEquals(2L, snapshot.etaSeconds)
        assertEquals(50.0, snapshot.progressPercent, 0.0001)
        assertEquals(2L, snapshot.elapsedSeconds)
    }

    @Test
    fun `zero elapsed time reports zero speed instead of infinity`() {
        val snapshot = SpeedCalculator.calculate(
            transferredBytes = 1024,
            totalBytes = 2048,
            startedAt = 5_000L,
            now = 5_000L
        )

        assertEquals(0.0, snapshot.speedMbps, 0.0)
        assertNull(snapshot.etaSeconds)
        assertEquals(0L, snapshot.elapsedSeconds)
    }

    @Test
    fun `clock going backwards is treated as zero elapsed`() {
        val snapshot = SpeedCalculator.calculate(
            transferredBytes = 512,
            totalBytes = 1024,
            startedAt = 10_000L,
            now = 9_000L
        )

        assertEquals(0.0, snapshot.speedMbps, 0.0)
        assertNull(snapshot.etaSeconds)
    }

    @Test
    fun `completed transfer has zero eta`() {
        val snapshot = SpeedCalculator.calculate(
            transferredBytes = 1024,
            totalBytes = 1024,
            startedAt = 0L,
            now = 1_000L
        )

        assertEquals(0L, snapshot.etaSeconds)
        assertEquals(100.0, snapshot.progressPercent, 0.0001)
    }

    @Test
    fun `no total bytes has no progress and no eta`() {
        val snapshot = SpeedCalculator.calculate(
            transferredBytes = 500,
            totalBytes = 0,
            startedAt = 0L,
            now = 1_000L
        )

        assertEquals(0.0, snapshot.progressPercent, 0.0)
        assertNull(snapshot.etaSeconds)
    }

    @Test
    fun `transferred more than total is clamped in progress and eta`() {
        val snapshot = SpeedCalculator.calculate(
            transferredBytes = 2048,
            totalBytes = 1024,
            startedAt = 0L,
            now = 1_000L
        )

        assertEquals(0L, snapshot.etaSeconds)
        assertEquals(200.0, snapshot.progressPercent, 0.0001)
    }

    @Test
    fun `speed grows as more bytes move in the same time`() {
        val slow = SpeedCalculator.calculate(1_000_000, 10_000_000, 0L, 1_000L)
        val fast = SpeedCalculator.calculate(5_000_000, 10_000_000, 0L, 1_000L)

        assertTrue(fast.speedMbps > slow.speedMbps)
        assertNotEquals(slow.speedMbps, fast.speedMbps)
    }
}

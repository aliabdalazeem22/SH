package com.example.swiftshare.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileNamingTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `plain name is unchanged`() {
        assertEquals("photo.jpg", FileNaming.sanitize("photo.jpg"))
    }

    @Test
    fun `directory components are stripped`() {
        assertEquals("passwd", FileNaming.sanitize("/etc/passwd"))
        assertEquals("passwd", FileNaming.sanitize("..\\..\\passwd"))
        assertEquals("file.txt", FileNaming.sanitize("../../dir/file.txt"))
    }

    @Test
    fun `traversal-only names become a safe default`() {
        assertEquals("received_file", FileNaming.sanitize(".."))
        assertEquals("received_file", FileNaming.sanitize("."))
        assertEquals("received_file", FileNaming.sanitize(""))
        assertEquals("received_file", FileNaming.sanitize(null))
        assertEquals("received_file", FileNaming.sanitize("   "))
    }

    @Test
    fun `control characters are removed`() {
        val sanitized = FileNaming.sanitize("bad\u0000name\n.txt")
        assertFalse(sanitized.contains("\u0000"))
        assertFalse(sanitized.contains("\n"))
        assertEquals("badname.txt", sanitized)
    }

    @Test
    fun `long names keep their extension`() {
        val longName = "a".repeat(300) + ".txt"
        val sanitized = FileNaming.sanitize(longName)

        assertEquals(180, sanitized.length)
        assertTrue(sanitized.endsWith(".txt"))
    }

    @Test
    fun `unique file returns the original when free`() {
        val dir = tempFolder.newFolder("downloads")
        val file = FileNaming.uniqueFile(dir, "report.pdf")

        assertEquals(File(dir, "report.pdf"), file)
    }

    @Test
    fun `unique file appends a counter on collision`() {
        val dir = tempFolder.newFolder("downloads")
        File(dir, "report.pdf").writeText("first")

        val second = FileNaming.uniqueFile(dir, "report.pdf")
        assertEquals("report (1).pdf", second.name)
        assertFalse(second.exists())

        second.writeText("second")
        val third = FileNaming.uniqueFile(dir, "report.pdf")
        assertEquals("report (2).pdf", third.name)
    }

    @Test
    fun `unique file handles names without extension`() {
        val dir = tempFolder.newFolder("downloads")
        File(dir, "NOEXT").writeText("data")

        val second = FileNaming.uniqueFile(dir, "NOEXT")
        assertEquals("NOEXT (1)", second.name)
    }

    @Test
    fun `unique file sanitizes before writing`() {
        val dir = tempFolder.newFolder("downloads")
        val file = FileNaming.uniqueFile(dir, "../escape.txt")

        assertEquals(dir, file.parentFile)
        assertEquals("escape.txt", file.name)
    }
}

package com.example.swiftshare.domain

import java.io.File

/**
 * Filesystem naming helpers, kept free of Android dependencies so they can be unit tested.
 *
 * Incoming file names come from another device and must be treated as untrusted:
 * separators and traversal segments are stripped before anything touches the filesystem.
 */
object FileNaming {

    /** Maximum length of a single file-name component on common Android filesystems. */
    private const val MAX_NAME_LENGTH = 180

    /**
     * Returns a safe file name (no directory components, no traversal, no control characters).
     * Never returns an empty string.
     */
    fun sanitize(rawName: String?): String {
        var name = (rawName ?: "").substringAfterLast('/').substringAfterLast('\\')
        name = name.replace("\u0000", "")
            .filter { it.code >= 32 && it != '/' && it != '\\' }
            .trim()
        if (name.isEmpty() || name == "." || name == "..") name = "received_file"
        if (name.length > MAX_NAME_LENGTH) {
            val dot = name.lastIndexOf('.')
            name = if (dot > 0 && name.length - dot <= 16) {
                val ext = name.substring(dot)
                name.substring(0, MAX_NAME_LENGTH - ext.length) + ext
            } else {
                name.substring(0, MAX_NAME_LENGTH)
            }
        }
        return name
    }

    /**
     * Returns a file inside [dir] with a unique name. If [name] already exists,
     * "name (1).ext", "name (2).ext", ... are tried.
     */
    fun uniqueFile(dir: File, name: String): File {
        val safeName = sanitize(name)
        val base = File(dir, safeName)
        if (!base.exists()) return base

        val dot = safeName.lastIndexOf('.')
        val namePart = if (dot > 0) safeName.substring(0, dot) else safeName
        val ext = if (dot > 0) safeName.substring(dot) else ""
        var index = 1
        while (index < 100_000) {
            val candidate = File(dir, "$namePart ($index)$ext")
            if (!candidate.exists()) return candidate
            index++
        }
        // Extremely unlikely fallback: append a timestamp.
        return File(dir, "$namePart-${System.currentTimeMillis()}$ext")
    }
}

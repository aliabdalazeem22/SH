package com.example.swiftshare.domain

import android.content.Context
import java.io.File

/**
 * Persists a minimal transfer history as JSON in the app's internal files dir.
 * File contents are never stored; only names, sizes, timestamps and status.
 */
class TransferHistoryStore(private val context: Context) {

    private companion object {
        /** Maximum number of metadata entries kept on disk. */
        const val MAX_RECORDS = 500
    }

    private val historyFile: File = File(context.filesDir, "transfer_history.json")

    private val records: MutableList<TransferRecord> = mutableListOf()

    init {
        load()
    }

    @Synchronized
    private fun load() {
        if (!historyFile.exists()) return
        historyFile.bufferedReader().use { reader ->
            val json = reader.readText()
            if (json.isBlank()) return@use
            try {
                val array = org.json.JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    records.add(TransferRecord(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        timestamp = obj.optLong("timestamp", 0),
                        direction = Direction.valueOf(obj.optString("direction", "SEND")),
                        role = Role.valueOf(obj.optString("role", "SENDER")),
                        fileName = obj.optString("fileName", ""),
                        fileSize = obj.optLong("fileSize", 0),
                        status = Status.valueOf(obj.optString("status", "IN_PROGRESS")),
                        peerName = obj.optString("peerName", null).takeIf { it.isNotEmpty() },
                        averageSpeedMbps = obj.optDouble("averageSpeedMbps", 0.0),
                        durationSeconds = obj.optLong("durationSeconds", 0)
                    ))
                }
            } catch (e: Exception) {
                // Corrupted history file; start fresh.
                records.clear()
            }
        }
    }

    @Synchronized
    private fun save() {
        val array = org.json.JSONArray()
        for (r in records) {
            val obj = org.json.JSONObject()
            obj.put("id", r.id)
            obj.put("timestamp", r.timestamp)
            obj.put("direction", r.direction.name)
            obj.put("role", r.role.name)
            obj.put("fileName", r.fileName)
            obj.put("fileSize", r.fileSize)
            obj.put("status", r.status.name)
            obj.put("peerName", r.peerName ?: org.json.JSONObject.NULL)
            obj.put("averageSpeedMbps", r.averageSpeedMbps)
            obj.put("durationSeconds", r.durationSeconds)
            array.put(obj)
        }
        historyFile.bufferedWriter().use { writer ->
            writer.write(array.toString(2))
        }
    }

    @Synchronized
    fun getRecords(): List<TransferRecord> = records.toList().reversed()

    @Synchronized
    fun addRecord(record: TransferRecord) {
        records.add(record)
        // Keep the local history bounded so the JSON file cannot grow forever.
        while (records.size > MAX_RECORDS) {
            records.removeAt(0)
        }
        save()
    }

    @Synchronized
    fun updateLastRecord(block: TransferRecord.() -> Unit) {
        if (records.isEmpty()) return
        val last = records.removeAt(records.size - 1)
        val updated = last.copy()
        updated.block()
        records.add(updated)
        save()
    }

    @Synchronized
    fun clear() {
        records.clear()
        historyFile.delete()
    }
}

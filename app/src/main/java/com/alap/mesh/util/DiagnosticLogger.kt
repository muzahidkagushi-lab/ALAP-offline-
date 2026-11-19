package com.alap.mesh.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticEntry(
    val timestampMillis: Long = System.currentTimeMillis(),
    val level: String, // INFO, TX, RX, RELAY, ACK, BLE, ERROR
    val tag: String,
    val message: String
) {
    fun formattedLine(): String {
        val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        return "[${fmt.format(Date(timestampMillis))}] [$level/$tag] $message"
    }
}

object DiagnosticLogger {
    private const val MAX_ENTRIES = 300
    private val entries = ArrayDeque<DiagnosticEntry>()
    private val _logs = MutableStateFlow<List<DiagnosticEntry>>(emptyList())
    val logs: StateFlow<List<DiagnosticEntry>> = _logs.asStateFlow()

    @Synchronized
    fun log(level: String, tag: String, message: String) {
        if (entries.size >= MAX_ENTRIES) {
            entries.removeFirst()
        }
        entries.addLast(DiagnosticEntry(level = level, tag = tag, message = message))
        _logs.value = entries.toList().asReversed()
    }

    @Synchronized
    fun clear() {
        entries.clear()
        _logs.value = emptyList()
    }

    @Synchronized
    fun exportAsText(): String {
        if (entries.isEmpty()) return "কোনো ডায়াগনস্টিক লগ নেই।"
        return entries.joinToString("\n") { it.formattedLine() }
    }
}

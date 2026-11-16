package com.lc.content.sdk.demo.lab

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** What a log line is about; the lab colors by it. */
enum class LogKind {
    /** A call the lab made into the SDK, and its direct result. */
    Api,

    /** A playback event the SDK delivered through `setPlayerEventListener`. */
    Event,

    /** A failed call or a playback error. */
    Error,
}

/** One line of the lab's log. [id] is unique within the process, for list keys. */
class LogLine(val id: Long, val kind: LogKind, val time: String, val message: String)

/**
 * The lines the lab shows, newest first. Snapshot state, so the screen follows it. Capped, so a long
 * session cannot grow it without bound.
 */
object LabLog {
    private const val MAX_LINES = 200
    private val clock = SimpleDateFormat("HH:mm:ss", Locale.US)
    private var nextId = 0L

    val lines = mutableStateListOf<LogLine>()

    fun add(kind: LogKind, message: String) {
        if (message.isBlank()) return
        lines.add(0, LogLine(nextId++, kind, clock.format(Date()), message))
        while (lines.size > MAX_LINES) lines.removeAt(lines.lastIndex)
    }

    fun clear() = lines.clear()
}

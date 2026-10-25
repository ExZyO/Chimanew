// EaZy Nuvio+ / Chimanew
package eu.kanade.tachiyomi.ui.home.chimanew.diagnostics

import android.util.Log
import eu.kanade.tachiyomi.ui.home.chimanew.ChimanewPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DiagnosticCategory(val code: String, val label: String) {
    NETWORK("network", "Network & Payloads"),
    ARTWORK("artwork", "Artwork Waterfall & Cache"),
    HERO("hero", "Hero Carousel & Strict Mode"),
    DETAILS("details", "Details Page & Backdrops"),
    SHELVES("shelves", "Home Shelves & Trackers"),
}

data class DiagnosticEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val category: DiagnosticCategory,
    val tag: String,
    val message: String,
    val error: String? = null,
) {
    private val timeStr: String get() = timeFormat.format(Date(timestamp))

    fun toFormattedString(): String {
        val err = if (!error.isNullOrBlank()) "\n  Exception: $error" else ""
        return "[$timeStr] [${category.code.uppercase()}] [$tag] $message$err"
    }

    companion object {
        private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    }
}

object ChimanewDiagnostics {
    private const val LOGCAT_TAG = "ChimanewDiag"
    private const val MAX_ENTRIES = 2500

    private val buffer = ArrayDeque<DiagnosticEntry>(MAX_ENTRIES)
    private val lock = Any()

    private val _entriesFlow = MutableStateFlow<List<DiagnosticEntry>>(emptyList())
    val entriesFlow = _entriesFlow.asStateFlow()

    @PublishedApi internal val preferences: ChimanewPreferences by lazy { Injekt.get() }

    inline fun log(
        category: DiagnosticCategory,
        tag: String,
        throwable: Throwable? = null,
        message: () -> String,
    ) {
        try {
            if (!preferences.enableDiagnostics().get()) return
            val activeCategories = preferences.diagnosticCategories().get()
            if (category.code !in activeCategories) return

            addEntry(category, tag, message(), throwable)
        } catch (_: Throwable) {
            // Fail silently to never disrupt core application functions
        }
    }

    inline fun log(
        category: DiagnosticCategory,
        tag: String,
        message: () -> String,
    ) {
        log(category, tag, null, message)
    }

    @PublishedApi internal fun addEntry(
        category: DiagnosticCategory,
        tag: String,
        msg: String,
        throwable: Throwable?,
    ) {
        val errString = throwable?.let { Log.getStackTraceString(it).trimEnd() }

        // 1. Android Logcat output
        if (errString != null) {
            Log.e(LOGCAT_TAG, "[${category.code.uppercase()}] [$tag] $msg\n$errString")
        } else {
            Log.d(LOGCAT_TAG, "[${category.code.uppercase()}] [$tag] $msg")
        }

        // 2. Thread-safe Ring Buffer
        val entry = DiagnosticEntry(
            category = category,
            tag = tag,
            message = msg,
            error = errString,
        )
        synchronized(lock) {
            if (buffer.size >= MAX_ENTRIES) {
                buffer.removeFirst()
            }
            buffer.addLast(entry)
            _entriesFlow.value = buffer.toList()
        }
    }

    fun getLogs(): List<DiagnosticEntry> = synchronized(lock) { buffer.toList() }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
            _entriesFlow.value = emptyList()
        }
    }

    fun exportText(): String {
        return synchronized(lock) {
            buffer.joinToString("\n") { it.toFormattedString() }
        }
    }
}

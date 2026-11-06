package app.zenith.zenboard

import android.content.ClipData
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** One copied text. [time] is wall-clock millis, so expiry survives a restart. */
data class Clip(val text: String, val time: Long, val pinned: Boolean = false)

/** How long unpinned clips are kept. Pinned clips stay until removed. */
enum class ClipRetention(val millis: Long) {
    HOUR(60 * 60 * 1000L),
    DAY(24 * 60 * 60 * 1000L),
    FOREVER(Long.MAX_VALUE),
}

/** Most unpinned clips kept, whatever the retention. */
const val MAX_CLIPS = 30

/** Longest text kept; a larger copy is stored cut to this length. */
const val MAX_CLIP_CHARS = 5_000

/** A clip this recent is offered as a one-tap paste in the toolbar (until typed past or ignored). */
const val FRESH_CLIP_MS = 60_000L

/**
 * [text] copied at [now]: newest first, an existing copy moves to the top (keeping its pin),
 * unpinned clips past [MAX_CLIPS] are dropped. Blank text changes nothing.
 */
fun addClip(clips: List<Clip>, text: String, now: Long): List<Clip> {
    if (text.isBlank()) return clips
    val kept = text.take(MAX_CLIP_CHARS)
    val old = clips.firstOrNull { it.text == kept }
    val added = listOf(Clip(kept, now, old?.pinned == true)) + clips.filter { it.text != kept }
    var unpinned = 0
    return added.filter { it.pinned || ++unpinned <= MAX_CLIPS }
}

/** Drops unpinned clips older than [retention]. */
fun pruneClips(clips: List<Clip>, now: Long, retention: ClipRetention): List<Clip> =
    if (retention == ClipRetention.FOREVER) clips else clips.filter { it.pinned || now - it.time <= retention.millis }

/** Pinned first, each part newest first: the panel order. */
fun panelOrder(clips: List<Clip>): List<Clip> = clips.sortedWith(compareByDescending<Clip> { it.pinned }.thenByDescending { it.time })

/** Text of a copy, or null when there is none or Android marks it sensitive (a copied password). */
fun clipText(data: ClipData?): String? {
    if (data == null || data.itemCount == 0) return null
    // ClipDescription.EXTRA_IS_SENSITIVE (API 33) by its literal name: password managers set it on
    // older Android versions too.
    if (data.description.extras?.getBoolean("android.content.extra.IS_SENSITIVE") == true) return null
    // Plain text only: no URIs or intents, which could point at other apps' data.
    return data.getItemAt(0).text?.toString()?.takeIf { it.isNotBlank() }
}

/**
 * Clears made in the settings screen, for a keyboard that is running in the same process: it drops
 * the clips from memory too. The value is true when pinned clips go as well.
 */
object ClipClears {
    val events = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
}

/**
 * Clipboard history on the device only: a JSON file in no-backup app storage. Read once when the
 * keyboard starts, written after each change, off the main thread.
 */
class ClipStore(context: Context) {
    private val file = File(context.noBackupFilesDir, "clipboard.json")
    private val meta = context.getSharedPreferences("clipboard", Context.MODE_PRIVATE)

    /**
     * When clips were last deleted (wall clock). A copy still on the system clipboard from before
     * then is not picked up again, so a deleted clip stays deleted.
     */
    var forgetBefore: Long
        get() = meta.getLong("forget_before", 0L)
        set(value) = meta.edit().putLong("forget_before", value).apply()

    suspend fun load(): List<Clip> = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) return@runCatching emptyList()
            val array = JSONArray(file.readText())
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                Clip(o.getString("text"), o.getLong("time"), o.optBoolean("pinned"))
            }
        }.getOrDefault(emptyList()) // An unreadable file starts an empty history instead of crashing the keyboard.
    }

    /** Deletes unpinned clips, or every clip with [includePinned], on disk and in a running keyboard. */
    suspend fun clear(includePinned: Boolean) {
        forgetBefore = System.currentTimeMillis()
        save(if (includePinned) emptyList() else load().filter(Clip::pinned))
        ClipClears.events.tryEmit(includePinned)
    }

    suspend fun save(clips: List<Clip>) = withContext(Dispatchers.IO) {
        if (clips.isEmpty()) {
            file.delete()
            return@withContext
        }
        val array = JSONArray()
        clips.forEach { array.put(JSONObject().put("text", it.text).put("time", it.time).put("pinned", it.pinned)) }
        // Write then rename, so a crash mid-write never leaves half a file.
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(array.toString())
        tmp.renameTo(file)
    }
}

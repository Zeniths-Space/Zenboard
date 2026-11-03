package app.zenith.keyboard

import android.content.Context
import android.graphics.Paint
import android.text.InputType
import android.view.inputmethod.EditorInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmojiCategory(val name: String, val icon: String, val emoji: List<String>)

/** Tab icons per Unicode group; unknown groups fall back to their first emoji. */
private val GROUP_ICONS = mapOf(
    "Smileys & Emotion" to "😀",
    "People & Body" to "👋",
    "Animals & Nature" to "🐻",
    "Food & Drink" to "🍔",
    "Travel & Places" to "✈️",
    "Activities" to "⚽",
    "Objects" to "💡",
    "Symbols" to "❤️",
    "Flags" to "🏁",
)

/** Parses assets/emoji.txt: "#" comment lines, then one "Group name<TAB>emoji emoji ..." line per group. */
fun parseEmoji(text: String): List<Pair<String, List<String>>> = text.lineSequence()
    .filter { it.isNotBlank() && !it.startsWith("#") }
    .mapNotNull { line ->
        val tab = line.indexOf('\t')
        if (tab < 0) null else line.substring(0, tab) to line.substring(tab + 1).split(' ').filter { it.isNotEmpty() }
    }
    .toList()

/** Loads the emoji list off the main thread, dropping emoji this device's font cannot draw. */
suspend fun loadEmoji(context: Context): List<EmojiCategory> = withContext(Dispatchers.Default) {
    val paint = Paint()
    val text = context.assets.open("emoji.txt").bufferedReader().use { it.readText() }
    parseEmoji(text).map { (name, all) ->
        val drawable = all.filter(paint::hasGlyph)
        EmojiCategory(name, GROUP_ICONS[name] ?: drawable.firstOrNull().orEmpty(), drawable)
    }.filter { it.emoji.isNotEmpty() }
}

/** Most recent first, no duplicates, capped. */
fun pushRecent(recents: List<String>, emoji: String, max: Int = 32): List<String> =
    (listOf(emoji) + recents.filter { it != emoji }).take(max)

/**
 * Recently used emoji, stored only on this device (app-private SharedPreferences, backups disabled).
 * Nothing is recorded in password fields or when the app asks for no personalized learning (incognito).
 */
class EmojiRecents(context: Context) {
    private val prefs = context.getSharedPreferences("emoji", Context.MODE_PRIVATE)

    fun load(): List<String> = prefs.getString(KEY, null)?.split('\n')?.filter { it.isNotEmpty() }.orEmpty()

    fun save(recents: List<String>) = prefs.edit().putString(KEY, recents.joinToString("\n")).apply()

    private companion object {
        const val KEY = "recent"
    }
}

/** True when the field asks the keyboard not to learn from it. */
fun isIncognito(info: EditorInfo?): Boolean {
    if (info == null) return true
    if (info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0) return true
    val variation = info.inputType and InputType.TYPE_MASK_VARIATION
    return when (info.inputType and InputType.TYPE_MASK_CLASS) {
        InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        else -> false
    }
}

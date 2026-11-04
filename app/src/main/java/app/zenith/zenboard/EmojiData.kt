package app.zenith.zenboard

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

/** One searchable emoji: its Unicode name and its CLDR keywords, split into lowercase words. */
class EmojiInfo(val emoji: String, val name: String, val nameWords: List<String>, val keywords: List<String>)

private val WORD_BREAK = Regex("[^\\p{L}\\p{N}'+]+")

private fun words(s: String) = s.split(WORD_BREAK).filter { it.isNotEmpty() }

/** Parses assets/emoji_keywords.txt: "#" comment lines, then "emoji<TAB>name<TAB>keywords" lines. */
fun parseEmojiKeywords(text: String): List<EmojiInfo> = text.lineSequence()
    .filter { it.isNotBlank() && !it.startsWith("#") }
    .mapNotNull { line ->
        val parts = line.split('\t')
        if (parts.size < 2) null else EmojiInfo(parts[0], parts[1], words(parts[1]), words(parts.getOrElse(2) { "" }))
    }
    .toList()

/** The search index, loaded off the main thread on the first search. Same glyph filter as [loadEmoji]. */
suspend fun loadEmojiIndex(context: Context): List<EmojiInfo> = withContext(Dispatchers.Default) {
    val paint = Paint()
    val text = context.assets.open("emoji_keywords.txt").bufferedReader().use { it.readText() }
    parseEmojiKeywords(text).filter { paint.hasGlyph(it.emoji) }
}

/**
 * Emoji where every query word starts some word of the name or keywords. Ranked: name starts with
 * the query, then all words found in the name, then matches through keywords; Unicode order within.
 */
fun searchEmoji(index: List<EmojiInfo>, query: String, max: Int = 60): List<String> {
    val q = query.trim().lowercase()
    val terms = words(q)
    if (terms.isEmpty()) return emptyList()
    fun List<String>.has(term: String) = any { it.startsWith(term) }
    return index.mapNotNull { e ->
        when {
            e.name.startsWith(q) -> 0
            terms.all { e.nameWords.has(it) } -> 1
            terms.all { e.nameWords.has(it) || e.keywords.has(it) } -> 2
            else -> null
        }?.let { it to e.emoji }
    }.sortedBy { it.first }.take(max).map { it.second }
}

/** Skin-tone variants per base emoji (light to dark), and the variant last picked for each base. */
class EmojiTones(val variants: Map<String, List<String>>, val chosen: Map<String, String> = emptyMap()) {
    private val bases: Map<String, String> = variants.flatMap { (base, tones) -> tones.map { it to base } }.toMap()

    /** The base of a toned emoji; any other emoji is its own base. */
    fun baseOf(emoji: String) = bases[emoji] ?: emoji

    /** What a cell shows and types for [emoji]: the tone picked for it, if any. */
    fun shown(emoji: String) = chosen[emoji] ?: emoji

    /** The base, then its five tones; empty when [emoji] has no tones. */
    fun options(emoji: String): List<String> {
        val base = baseOf(emoji)
        return variants[base]?.let { listOf(base) + it }.orEmpty()
    }

    /** [emoji] was typed: it becomes the remembered pick for its base. Same instance when nothing changes. */
    fun withChoice(emoji: String): EmojiTones {
        val base = baseOf(emoji)
        if (base !in variants || shown(base) == emoji) return this
        return EmojiTones(variants, chosen + (base to emoji))
    }

    companion object {
        val NONE = EmojiTones(emptyMap())
    }
}

/** Parses assets/emoji_tones.txt: "#" comment lines, then "base<TAB>tone tone tone tone tone" lines. */
fun parseEmojiTones(text: String): Map<String, List<String>> = parseEmoji(text).toMap()

/** Tone variants the device font can draw; loaded with the emoji list. */
suspend fun loadEmojiTones(context: Context): Map<String, List<String>> = withContext(Dispatchers.Default) {
    val paint = Paint()
    val text = context.assets.open("emoji_tones.txt").bufferedReader().use { it.readText() }
    parseEmojiTones(text).mapValues { (_, tones) -> tones.filter(paint::hasGlyph) }.filterValues { it.isNotEmpty() }
}

/** Most recent first, no duplicates, capped. */
fun pushRecent(recents: List<String>, emoji: String, max: Int = 32): List<String> =
    (listOf(emoji) + recents.filter { it != emoji }).take(max)

/**
 * Recently used emoji and picked skin tones, stored only on this device (app-private SharedPreferences, backups disabled).
 * Nothing is recorded in password fields or when the app asks for no personalized learning (incognito).
 */
class EmojiRecents(context: Context) {
    private val prefs = context.getSharedPreferences("emoji", Context.MODE_PRIVATE)

    fun load(): List<String> = prefs.getString(KEY, null)?.split('\n')?.filter { it.isNotEmpty() }.orEmpty()

    fun save(recents: List<String>) = prefs.edit().putString(KEY, recents.joinToString("\n")).apply()

    /** Picked skin tones: base emoji to the variant last typed. */
    fun loadTones(): Map<String, String> = prefs.getString(TONES, null)?.split('\n')
        ?.mapNotNull { line -> line.split('\t').takeIf { it.size == 2 }?.let { it[0] to it[1] } }
        ?.toMap().orEmpty()

    fun saveTones(chosen: Map<String, String>) =
        prefs.edit().putString(TONES, chosen.entries.joinToString("\n") { "${it.key}\t${it.value}" }).apply()

    private companion object {
        const val KEY = "recent"
        const val TONES = "tones"
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

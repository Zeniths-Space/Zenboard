package app.zenith.zenboard

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.ln

/**
 * In-memory word list for suggestions and autocorrect. Pure logic (no Android calls) apart from [load].
 *
 * Words are kept in one array sorted by lowercase key, so lookups and prefix ranges are binary
 * searches: no hash map, which keeps the always-resident IME process small.
 */
class Dictionary(
    /** (word, frequency 0-255) sorted by lowercase word. */
    entries: List<Pair<String, Int>>,
    /** Curated misspelling (lowercase) -> correction. */
    private val corrections: Map<String, String> = emptyMap(),
) {
    private val keys = Array(entries.size) { entries[it].first.lowercase() }
    private val words = Array(entries.size) { entries[it].first }
    private val freqs = IntArray(entries.size) { entries[it].second }

    /** Words the user kept (reverted an autocorrect or tapped the typed word). */
    private val learned = HashSet<String>()

    fun learn(word: String) {
        learned += word.lowercase()
    }

    fun learnAll(words: Collection<String>) = words.forEach(::learn)

    /** First index whose key is >= [key]. */
    private fun lowerBound(key: String): Int {
        var lo = 0
        var hi = keys.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (keys[mid] < key) lo = mid + 1 else hi = mid
        }
        return lo
    }

    private inline fun forEachWithKey(key: String, action: (Int) -> Unit) {
        var i = lowerBound(key)
        while (i < keys.size && keys[i] == key) action(i++)
    }

    /**
     * Known as typed. Case matters only one way: "Hello" matches "hello", but "i" does not match
     * "I" and "monday" does not match "Monday", so those still get corrected to their capital form.
     */
    fun isKnown(typed: String): Boolean {
        val key = typed.lowercase()
        if (key in learned) return true
        var known = false
        forEachWithKey(key) { i ->
            val word = words[i]
            if (word == key || word == typed || typed.first().isUpperCase()) known = true
        }
        return known
    }

    /** Every word whose lowercase form starts with [first], with its frequency (0-255). */
    fun forEachStartingWith(first: Char, action: (word: String, frequency: Int) -> Unit) {
        val key = first.toString()
        var i = lowerBound(key)
        while (i < keys.size && keys[i].startsWith(key)) {
            action(words[i], freqs[i])
            i++
        }
    }

    /** Highest-frequency spelling for [key] (lowercase), or null. */
    private fun best(key: String): Int? {
        var best: Int? = null
        forEachWithKey(key) { i -> if (best == null || freqs[i] > freqs[best!!]) best = i }
        return best
    }

    /** Completions of [prefix], most frequent first. Includes the word itself when it is a word. */
    fun completions(prefix: String, limit: Int): List<String> = completionCandidates(prefix, limit).map { it.word }

    /** As [completions], with frequencies; distance is the number of letters still to type. */
    fun completionCandidates(prefix: String, limit: Int): List<Candidate> {
        if (prefix.isEmpty()) return emptyList()
        val key = prefix.lowercase()
        val start = lowerBound(key)
        val top = ArrayList<Int>(limit + 1)
        var i = start
        while (i < keys.size && keys[i].startsWith(key)) {
            val f = freqs[i]
            // Keep a small sorted top-N; ranges for one-letter prefixes are a few thousand words.
            if (top.size < limit || f > freqs[top.last()]) {
                var at = top.size
                while (at > 0 && freqs[top[at - 1]] < f) at--
                top.add(at, i)
                if (top.size > limit) top.removeAt(top.lastIndex)
            }
            i++
        }
        return top.map { Candidate(words[it], (words[it].length - prefix.length).toDouble(), freqs[it]) }
    }

    /**
     * Likely intended words for a [typed] word that is not known, best first, with a weighted
     * edit distance (adjacent-key substitutions and transpositions cost less).
     */
    fun corrections(typed: String, limit: Int = 3): List<Candidate> {
        val key = typed.lowercase()
        corrections[key]?.let { return listOf(Candidate(it, 0.0, 255)) }
        val found = HashMap<String, Candidate>()
        fun consider(i: Int, distance: Double) {
            val word = words[i]
            val old = found[word]
            if (old == null || distance < old.distance) found[word] = Candidate(word, distance, freqs[i])
        }
        // Same letters, different case: "i" -> "I", "monday" -> "Monday".
        best(key)?.let { consider(it, 0.0) }
        // One edit anywhere, including the first letter, scored with the typing-aware weights.
        for (edit in edits1(key)) best(edit)?.let { consider(it, weightedDistance(key, keys[it])) }
        // Up to two weighted edits for words sharing the first letter (most typos keep it).
        if (key.length >= 3) {
            val first = key.substring(0, 1)
            var i = lowerBound(first)
            while (i < keys.size && keys[i].startsWith(first)) {
                val candidate = keys[i]
                if (abs(candidate.length - key.length) <= 2) {
                    val d = weightedDistance(key, candidate, cutoff = 2.0)
                    if (d <= 2.0) consider(i, d)
                }
                i++
            }
        }
        return found.values.sortedByDescending { it.score }.take(limit)
    }

    class Candidate(val word: String, val distance: Double, val frequency: Int) {
        /** Frequency in log space, minus a strong penalty per edit. */
        val score: Double get() = ln(1.0 + frequency) - 2.2 * distance
    }

    private fun edits1(word: String): Sequence<String> = sequence {
        val letters = 'a'..'z'
        for (i in word.indices) yield(word.removeRange(i, i + 1))
        for (i in 0 until word.length - 1) {
            yield(word.substring(0, i) + word[i + 1] + word[i] + word.substring(i + 2))
        }
        for (i in word.indices) for (c in letters) if (c != word[i]) {
            yield(word.substring(0, i) + c + word.substring(i + 1))
        }
        for (i in 0..word.length) for (c in letters) yield(word.substring(0, i) + c + word.substring(i))
        for (i in 0..word.length) yield(word.substring(0, i) + "'" + word.substring(i))
    }

    companion object {
        /** Parses the dict asset format (see its header). */
        fun parse(text: String): Dictionary {
            val entries = ArrayList<Pair<String, Int>>(64_000)
            val corrections = HashMap<String, String>()
            for (line in text.lineSequence()) {
                if (line.isEmpty() || line[0] == '#') continue
                val parts = line.split('\t')
                if (parts.size != 3) continue
                when (parts[0]) {
                    "w" -> parts[2].toIntOrNull()?.let { entries += parts[1] to it }
                    "c" -> corrections[parts[1]] = parts[2]
                }
            }
            return Dictionary(entries, corrections)
        }

        suspend fun load(context: Context): Dictionary = withContext(Dispatchers.Default) {
            parse(context.assets.open("dict_en_US.txt").bufferedReader().use { it.readText() })
        }
    }
}

/** QWERTY key positions (row, column with stagger), for adjacent-key costs. */
private val KEY_POSITIONS: Map<Char, Pair<Float, Float>> = buildMap {
    listOf("qwertyuiop" to 0f, "asdfghjkl" to 0.5f, "zxcvbnm" to 1.5f).forEachIndexed { row, (keys, offset) ->
        keys.forEachIndexed { col, c -> put(c, row.toFloat() to col + offset) }
    }
}

/** True for keys that touch on the QWERTY layout. */
fun adjacentKeys(a: Char, b: Char): Boolean {
    val pa = KEY_POSITIONS[a] ?: return false
    val pb = KEY_POSITIONS[b] ?: return false
    return abs(pa.first - pb.first) <= 1f && abs(pa.second - pb.second) <= 1f
}

/**
 * Damerau-Levenshtein distance with typing-aware costs: apostrophe 0.2, doubling or undoubling a letter 0.4
 * ("helo" -> "hello"), transposition 0.5, neighbouring-key substitution 0.6, other edits 1.0.
 * Stops early once every path exceeds [cutoff].
 */
fun weightedDistance(a: String, b: String, cutoff: Double = Double.MAX_VALUE): Double {
    val n = a.length
    val m = b.length
    var prev2 = DoubleArray(m + 1)
    var prev = DoubleArray(m + 1) { it.toDouble() }
    var cur = DoubleArray(m + 1)
    for (i in 1..n) {
        cur[0] = i.toDouble()
        var rowMin = cur[0]
        for (j in 1..m) {
            val sub = when {
                a[i - 1] == b[j - 1] -> 0.0
                adjacentKeys(a[i - 1], b[j - 1]) -> 0.6
                else -> 1.0
            }
            // Deleting a[i-1] / inserting b[j-1] is cheap when it repeats the previous letter.
            // A missing or extra apostrophe ("dont" -> "don't") is nearly free.
            val del = when {
                a[i - 1] == '\'' -> 0.2
                i > 1 && a[i - 1] == a[i - 2] -> 0.4
                else -> 1.0
            }
            val ins = when {
                b[j - 1] == '\'' -> 0.2
                j > 1 && b[j - 1] == b[j - 2] -> 0.4
                else -> 1.0
            }
            var v = minOf(prev[j] + del, cur[j - 1] + ins, prev[j - 1] + sub)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) v = minOf(v, prev2[j - 2] + 0.5)
            cur[j] = v
            if (v < rowMin) rowMin = v
        }
        if (rowMin > cutoff) return rowMin
        val t = prev2
        prev2 = prev
        prev = cur
        cur = t
    }
    return prev[m]
}

/** Same casing as what was typed: "HELLO" -> all caps, "Hello" -> capitalised, else the word's own case. */
fun matchCase(word: String, typed: String): String = when {
    typed.length >= 2 && typed.all { !it.isLetter() || it.isUpperCase() } -> word.uppercase()
    typed.firstOrNull()?.isUpperCase() == true -> word.replaceFirstChar { it.uppercaseChar() }
    else -> word
}

/**
 * What the suggestion strip shows for [typed]. [primary] goes in the centre (and is what
 * autocorrect would type); [others] go either side. [typedIsUnknown] marks [typed] as offered
 * verbatim so the user can keep it.
 */
class Suggestions(val primary: String?, val others: List<String>, val autocorrect: String?, val typedIsUnknown: Boolean) {
    companion object {
        val NONE = Suggestions(null, emptyList(), null, false)
    }
}

/** Autocorrect only when the best fix is close: one edit, or up to ~1.6 weighted edits in longer words. */
private fun confident(best: Dictionary.Candidate, typed: String): Boolean =
    best.distance <= 1.0 || (typed.length >= 5 && best.distance <= 1.6)

fun suggest(dict: Dictionary, typed: String, allowAutocorrect: Boolean): Suggestions {
    if (typed.isEmpty()) return Suggestions.NONE
    val known = dict.isKnown(typed)
    val fixes = if (known) emptyList() else dict.corrections(typed, 3)
    val auto = fixes.firstOrNull()
        // One letter is only ever corrected for "i" -> "I"; "u" or "a" are left alone.
        ?.takeIf { allowAutocorrect && (typed.length >= 2 || typed == "i") && confident(it, typed) }
        ?.word?.let { matchCase(it, typed) }
    // Fixes and completions compete on one score; each letter a completion still needs costs a little,
    // so "hel" offers "help" and "hello" before the typo fix "hell".
    val scores = HashMap<String, Double>()
    fun offer(word: String, score: Double) = scores.merge(word, score, ::maxOf)
    fixes.forEach { offer(it.word, it.score) }
    dict.completionCandidates(typed, 6).forEach { offer(it.word, ln(1.0 + it.frequency) - 0.3 * minOf(it.distance, 3.0)) }
    val ranked = (if (known) listOf(typed) else emptyList()) +
        listOfNotNull(auto) +
        scores.entries.sortedByDescending { it.value }.map { it.key }
    val words = ranked.map { matchCase(it, typed) }.distinct().filter { it != typed || known }
    return if (known) {
        Suggestions(primary = typed, others = words.drop(1).take(2), autocorrect = null, typedIsUnknown = false)
    } else {
        // Unknown word: offer it verbatim on one side so it can be kept.
        Suggestions(primary = words.firstOrNull(), others = listOf(typed) + words.drop(1).take(1), autocorrect = auto, typedIsUnknown = true)
    }
}

/**
 * Words the user chose to keep (undid an autocorrect, or tapped the typed word), stored only on this
 * device. Never written from password or incognito fields.
 */
class LearnedWords(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("learned_words", Context.MODE_PRIVATE)

    fun load(): Set<String> = prefs.getStringSet(KEY, emptySet()).orEmpty()

    fun add(word: String) {
        val words = load().toMutableSet()
        if (!words.add(word.lowercase())) return
        // shortcut: keeps an arbitrary subset past the cap; switch to least-recently-used if it matters.
        prefs.edit().putStringSet(KEY, words.take(MAX).toSet()).apply()
    }

    private companion object {
        const val KEY = "words"
        const val MAX = 1000
    }
}

package app.zenith.zenboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.ln

/** Where the letter keys are: centre of each key (root coordinates, px) and a typical key width. */
class KeyGeometry(val centers: Map<Char, Offset>, val keyWidth: Float)

/** Points per resampled path. Enough to keep corners of long words, cheap to compare. */
private const val SAMPLES = 40

/**
 * Glide typing: the words whose key path best matches the finger's [path], best first.
 *
 * Shape matching in the style of SHARK2's location channel: the finger path and each candidate
 * word's ideal path (straight lines through its key centres) are resampled to [SAMPLES] evenly
 * spaced points, and their mean distance, in key widths, is the cost. Word frequency breaks near
 * ties. Candidates are only words starting near the first point and ending near the last, and
 * every letter must lie near the path, which leaves a few hundred words of the 60,000 to compare.
 */
fun decodeGlide(path: List<Offset>, geo: KeyGeometry, dict: Dictionary, limit: Int = 3): List<String> {
    if (path.size < 2 || geo.centers.isEmpty()) return emptyList()
    val kw = geo.keyWidth
    val finger = resample(path, SAMPLES)
    val starts = nearestKeys(path.first(), geo, 3)
    val ends = nearestKeys(path.last(), geo, 3).toSet()
    // Each letter's distance to the path, computed once per letter.
    val nearPath = geo.centers.mapValues { (_, c) -> finger.minOf { (it - c).getDistance() } <= kw * 1.25f }

    class Scored(val word: String, val score: Double)
    val best = ArrayList<Scored>()
    val seen = HashSet<String>()
    for (first in starts) {
        dict.forEachStartingWith(first) { word, freq ->
            val letters = word.lowercase().filter { it != '\'' }
            if (letters.length < 2 || letters.last() !in ends) return@forEachStartingWith
            if (!letters.all { nearPath[it] == true }) return@forEachStartingWith
            val template = templateFor(letters, geo) ?: return@forEachStartingWith
            val cost = meanDistance(finger, resample(template, SAMPLES)) / kw
            // Cost dominates; frequency (ln, up to ~5.5) decides between close shapes. Names and
            // other capitalised entries ("God", "Scott") rank below ordinary words.
            val capital = if (word.first().isUpperCase()) 1.0 else 0.0
            best += Scored(word, -6.0 * cost + 0.8 * ln(1.0 + freq) - capital)
        }
    }
    // One spelling per word: the best-scoring of "god" and "God".
    return best.sortedByDescending { it.score }.filter { seen.add(it.word.lowercase()) }.take(limit).map { it.word }
}

/** Key centres of [letters], with repeated letters ("ll" in "hello") collapsed to one point. */
private fun templateFor(letters: String, geo: KeyGeometry): List<Offset>? {
    val points = ArrayList<Offset>(letters.length)
    var prev = ' '
    for (c in letters) {
        if (c == prev) continue
        points += geo.centers[c] ?: return null
        prev = c
    }
    // One distinct key ("ll"): a dot. Two copies so resampling has a segment to walk.
    if (points.size == 1) points += points[0]
    return points
}

private fun nearestKeys(p: Offset, geo: KeyGeometry, n: Int): List<Char> =
    geo.centers.entries.sortedBy { (it.value - p).getDistance() }.take(n).map { it.key }

private fun meanDistance(a: List<Offset>, b: List<Offset>): Float {
    var sum = 0f
    for (i in a.indices) sum += (a[i] - b[i]).getDistance()
    return sum / a.size
}

/** [n] points evenly spaced along the polyline [points]. */
fun resample(points: List<Offset>, n: Int): List<Offset> {
    var total = 0f
    for (i in 1 until points.size) total += (points[i] - points[i - 1]).getDistance()
    if (total == 0f) return List(n) { points.first() }
    val step = total / (n - 1)
    val out = ArrayList<Offset>(n)
    out += points.first()
    var carried = 0f
    var i = 1
    var from = points[0]
    while (i < points.size && out.size < n) {
        val to = points[i]
        val seg = hypot(to.x - from.x, to.y - from.y)
        if (carried + seg >= step && seg > 0f) {
            val t = (step - carried) / seg
            val p = Offset(from.x + t * (to.x - from.x), from.y + t * (to.y - from.y))
            out += p
            from = p
            carried = 0f
        } else {
            carried += seg
            from = to
            i++
        }
    }
    while (out.size < n) out += points.last()
    return out
}

/** Shared by the keyboard and its keys while glide typing is on. */
@Stable
class GlideState {
    /** True while a glide is in progress: the key it started on stays quiet and types nothing. */
    var active by mutableStateOf(false)

    /** The finger trail of the current glide, in the keyboard's own coordinates. Read only when drawing. */
    val trail = mutableStateListOf<Offset>()

    /** Letter key rects in root coordinates, kept up to date by layout. */
    val keys = HashMap<Char, Rect>()

    fun geometry(): KeyGeometry? {
        if (keys.size < 26) return null
        val widths = keys.values.map { it.width }.sorted()
        return KeyGeometry(keys.mapValues { it.value.center }, widths[widths.size / 2])
    }

    /** The letter key under [p] (root), counting half the gap around each key. */
    fun letterAt(p: Offset, slack: Float): Char? = keys.entries.firstOrNull { it.value.inflate(slack).contains(p) }?.key
}

/**
 * Glide typing over the whole keyboard. Watches each gesture in the Initial pass, before the keys:
 * a finger that starts on a letter and travels more than [START_KEYS] key widths becomes a glide.
 * From then on its events are consumed, so the keys see a cancelled touch and type nothing. Taps,
 * long-presses (the alternates panel), and gestures with a second finger down are left to the keys.
 */
@Composable
fun Modifier.glideGestures(
    glide: GlideState,
    enabled: Boolean,
    panelOpen: () -> Boolean,
    toRoot: (Offset) -> Offset,
    onGlide: (path: List<Offset>, geo: KeyGeometry) -> Unit,
): Modifier {
    val on by rememberUpdatedState(enabled)
    val panel by rememberUpdatedState(panelOpen)
    val root by rememberUpdatedState(toRoot)
    val done by rememberUpdatedState(onGlide)
    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (!on) return@awaitEachGesture
            val geo = glide.geometry() ?: return@awaitEachGesture
            if (glide.letterAt(root(down.position), slack = 3.dp.toPx()) == null) return@awaitEachGesture
            val threshold = geo.keyWidth * START_KEYS
            val points = arrayListOf(down.position)
            var gliding = false
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!gliding) {
                        // A second finger means fast tapping, not a glide.
                        if (event.changes.any { it.id != down.id && it.pressed }) return@awaitEachGesture
                        if (change.changedToUpIgnoreConsumed()) return@awaitEachGesture
                        points += change.position
                        if ((change.position - down.position).getDistance() > threshold && !panel()) {
                            gliding = true
                            glide.active = true
                            glide.trail.addAll(points)
                        }
                    } else {
                        change.consume()
                        if (change.changedToUpIgnoreConsumed()) break
                        points += change.position
                        glide.trail += change.position
                    }
                }
                if (gliding) done(points.map(root), geo)
            } finally {
                if (gliding) {
                    glide.active = false
                    glide.trail.clear()
                }
            }
        }
    }
}

/** How far, in key widths, a finger must travel from its first key before a touch becomes a glide. */
private const val START_KEYS = 0.75f

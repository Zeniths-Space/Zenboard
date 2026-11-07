package com.elara.keyboard

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class GlideTypingTest {

    /** QWERTY with 100 px keys, rows 100 px apart, staggered like the real layout. */
    private val geo = KeyGeometry(
        buildMap {
            listOf("qwertyuiop" to 0f, "asdfghjkl" to 0.5f, "zxcvbnm" to 1.5f).forEachIndexed { row, (keys, offset) ->
                keys.forEachIndexed { col, c -> put(c, Offset((col + offset) * 100f + 50f, row * 100f + 50f)) }
            }
        },
        keyWidth = 100f,
    )

    private val real: Dictionary? by lazy {
        File("src/main/assets/dict_en_US.txt").takeIf { it.exists() }?.let { Dictionary.parse(it.readText()) }
    }

    /**
     * A finger path through the word's keys in straight strokes. Each key is hit up to [jitter] px
     * off its centre, the way a finger misses: smooth strokes, not a zigzag.
     */
    private fun swipe(word: String, jitter: Float, seed: Int = 1): List<Offset> {
        val rnd = Random(seed)
        val keys = word.map { geo.centers.getValue(it) + Offset(rnd.nextFloat() * 2 * jitter - jitter, rnd.nextFloat() * 2 * jitter - jitter) }
        val out = ArrayList<Offset>()
        for (i in 1 until keys.size) {
            for (s in 0 until 8) out += keys[i - 1] + (keys[i] - keys[i - 1]) * (s / 8f)
        }
        out += keys.last()
        return out
    }

    @Test
    fun `resample spaces points evenly and keeps the ends`() {
        val r = resample(listOf(Offset(0f, 0f), Offset(10f, 0f), Offset(10f, 10f)), 5)
        assertEquals(5, r.size)
        assertEquals(Offset(0f, 0f), r.first())
        assertEquals(10f, r.last().x, 0.01f)
        assertEquals(10f, r.last().y, 0.01f)
        assertEquals(Offset(10f, 0f), r[2])
    }

    @Test
    fun `clean swipes decode to the intended word`() {
        val dict = real ?: return
        val words = listOf("hello", "the", "keyboard", "good", "you", "quick", "thanks", "where", "people", "about")
        val misses = words.filter { decodeGlide(swipe(it, jitter = 0f), geo, dict).firstOrNull()?.lowercase() != it }
        assertEquals(emptyList<String>(), misses)
    }

    @Test
    fun `sloppy swipes still find the word in the top three`() {
        val dict = real ?: return
        // "typing" and "topping" trace the same line along the top row, so it is not in this list.
        val words = listOf("hello", "keyboard", "thanks", "people", "about", "good", "morning", "tomorrow", "great", "home")
        val misses = words.filter { w ->
            (1..5).any { seed -> w !in decodeGlide(swipe(w, jitter = 35f, seed), geo, dict).map { it.lowercase() } }
        }
        assertEquals(emptyList<String>(), misses)
    }

    @Test
    fun `a path that hits no word gives nothing rather than a crash`() {
        val dict = Dictionary(listOf("hello" to 100))
        assertTrue(decodeGlide(swipe("qp", 0f), geo, dict).isEmpty())
        assertTrue(decodeGlide(listOf(Offset(50f, 50f)), geo, dict).isEmpty())
    }
}

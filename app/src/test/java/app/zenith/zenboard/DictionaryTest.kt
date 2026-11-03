package app.zenith.zenboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DictionaryTest {

    private val small = Dictionary(
        listOf("hello" to 120, "help" to 150, "helped" to 90, "here" to 180, "I" to 196, "Monday" to 120, "the" to 222)
            .sortedBy { it.first.lowercase() },
        corrections = mapOf("teh" to "the"),
    )

    /** The real asset, when unit tests run from the module directory. */
    private val real: Dictionary? by lazy {
        File("src/main/assets/dict_en_US.txt").takeIf { it.exists() }?.let { Dictionary.parse(it.readText()) }
    }

    @Test
    fun `case only matters towards capitals`() {
        assertTrue(small.isKnown("hello"))
        assertTrue(small.isKnown("Hello"))
        assertFalse(small.isKnown("i"))
        assertFalse(small.isKnown("monday"))
        assertTrue(small.isKnown("Monday"))
    }

    @Test
    fun `completions are prefix matches, most frequent first`() {
        assertEquals(listOf("here", "help", "hello"), small.completions("he", 3))
        assertEquals(listOf("help", "helped"), small.completions("help", 3))
        assertEquals(emptyList<String>(), small.completions("xyz", 3))
    }

    @Test
    fun `corrections use curated fixes, capitals and weighted edits`() {
        assertEquals("the", small.corrections("teh").first().word)
        assertEquals("I", small.corrections("i").first().word)
        assertEquals("Monday", small.corrections("monday").first().word)
        assertEquals("hello", small.corrections("helo").first().word)
    }

    @Test
    fun `weighted distance favours doubled letters, neighbours and swaps`() {
        assertEquals(0.4, weightedDistance("helo", "hello"), 1e-9)
        assertEquals(0.6, weightedDistance("hwllo", "hello"), 1e-9)
        assertEquals(0.5, weightedDistance("hlelo", "hello"), 1e-9)
        assertEquals(1.0, weightedDistance("hpllo", "hello"), 1e-9)
        assertTrue(adjacentKeys('e', 'w'))
        assertFalse(adjacentKeys('q', 'p'))
    }

    @Test
    fun `case of suggestions follows what was typed`() {
        assertEquals("HELLO", matchCase("hello", "HEL"))
        assertEquals("Hello", matchCase("hello", "Hel"))
        assertEquals("Monday", matchCase("Monday", "mon"))
    }

    @Test
    fun `known words are offered as typed, unknown words verbatim beside the fix`() {
        val known = suggest(small, "help", allowAutocorrect = true)
        assertEquals("help", known.primary)
        assertNull(known.autocorrect)
        val typo = suggest(small, "helo", allowAutocorrect = true)
        assertEquals("hello", typo.primary)
        assertEquals("hello", typo.autocorrect)
        assertEquals("helo", typo.others.first())
        assertNull(suggest(small, "helo", allowAutocorrect = false).autocorrect)
    }

    @Test
    fun `learned words are no longer corrected`() {
        val dict = Dictionary(listOf("hello" to 120))
        assertFalse(dict.isKnown("zenboard"))
        dict.learn("Zenboard")
        assertTrue(dict.isKnown("zenboard"))
    }

    @Test
    fun `real dictionary fixes common typos`() {
        val dict = real ?: return
        for ((typo, fix) in listOf("teh" to "the", "dont" to "don't", "im" to "I'm", "recieve" to "receive", "helo" to "hello", "thnaks" to "thanks", "i" to "I")) {
            assertEquals(typo, fix, suggest(dict, typo, allowAutocorrect = true).autocorrect)
        }
        for (word in listOf("hello", "keyboard", "the", "Monday", "don't")) {
            assertNull(word, suggest(dict, word, allowAutocorrect = true).autocorrect)
        }
    }

    @Test
    fun `a partial word offers completions before typo fixes`() {
        val dict = real ?: return
        val s = suggest(dict, "hel", allowAutocorrect = false)
        // A real completion ("held", "help", "hello"...), not the one-letter typo fix "hell".
        assertTrue(s.primary, s.primary!!.startsWith("hel") && s.primary != "hell")
    }

    @Test
    fun `single letters other than i are never autocorrected`() {
        val dict = real ?: return
        assertEquals("I", suggest(dict, "i", allowAutocorrect = true).autocorrect)
        assertNull(suggest(dict, "u", allowAutocorrect = true).autocorrect)
    }
}

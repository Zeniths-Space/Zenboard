package app.zenith.zenboard

import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardHistoryTest {

    @Test
    fun `new clips go first and a repeat moves up keeping its pin`() {
        var clips = addClip(emptyList(), "one", now = 1)
        clips = addClip(clips, "two", now = 2)
        assertEquals(listOf("two", "one"), clips.map { it.text })
        clips = clips.map { if (it.text == "one") it.copy(pinned = true) else it }
        clips = addClip(clips, "one", now = 3)
        assertEquals(listOf("one", "two"), clips.map { it.text })
        assertEquals(true, clips.first().pinned)
        assertEquals(3L, clips.first().time)
    }

    @Test
    fun `blank copies are ignored and long ones are cut`() {
        assertEquals(emptyList<Clip>(), addClip(emptyList(), "  \n ", now = 1))
        assertEquals(MAX_CLIP_CHARS, addClip(emptyList(), "x".repeat(MAX_CLIP_CHARS + 10), now = 1).single().text.length)
    }

    @Test
    fun `only unpinned clips count towards the cap`() {
        var clips = listOf(Clip("pinned", 0, pinned = true))
        for (i in 1..MAX_CLIPS + 5) clips = addClip(clips, "clip $i", now = i.toLong())
        assertEquals(MAX_CLIPS, clips.count { !it.pinned })
        assertEquals(true, clips.any { it.text == "pinned" })
        assertEquals(false, clips.any { it.text == "clip 1" })
    }

    @Test
    fun `retention drops old unpinned clips only`() {
        val hour = ClipRetention.HOUR.millis
        val clips = listOf(Clip("new", hour * 2), Clip("old", 0), Clip("old pinned", 0, pinned = true))
        assertEquals(listOf("new", "old pinned"), pruneClips(clips, now = hour * 2, ClipRetention.HOUR).map { it.text })
        assertEquals(3, pruneClips(clips, now = hour * 2, ClipRetention.DAY).size)
        assertEquals(3, pruneClips(clips, now = Long.MAX_VALUE / 2, ClipRetention.FOREVER).size)
    }

    @Test
    fun `panel shows pinned first, each newest first`() {
        val clips = listOf(Clip("a", 1), Clip("b", 3), Clip("p1", 2, pinned = true), Clip("p2", 0, pinned = true))
        assertEquals(listOf("p1", "p2", "b", "a"), panelOrder(clips).map { it.text })
    }
}

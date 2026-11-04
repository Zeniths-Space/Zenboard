package app.zenith.zenboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyboardModelTest {

    @Test
    fun `single shift tap capitalizes one letter`() {
        val shifted = KeyboardState().tapShift(now = 0)
        assertEquals("A", shifted.textFor(Key.Text("a")))
        assertEquals(ShiftMode.OFF, shifted.afterText().shift)
    }

    @Test
    fun `double tap locks caps and lock survives typing`() {
        val locked = KeyboardState().tapShift(0).tapShift(DOUBLE_TAP_MS)
        assertEquals(ShiftMode.LOCKED, locked.shift)
        assertEquals(ShiftMode.LOCKED, locked.afterText().shift)
        assertEquals(ShiftMode.LOCKED, locked.withAutoCaps(capsOn = false).shift)
        assertEquals(ShiftMode.OFF, locked.tapShift(10_000).shift)
    }

    @Test
    fun `slow second tap turns shift off`() {
        assertEquals(ShiftMode.OFF, KeyboardState().tapShift(0).tapShift(DOUBLE_TAP_MS + 1).shift)
    }

    @Test
    fun `tapping shift after auto caps turns it off, not on lock`() {
        val auto = KeyboardState().withAutoCaps(capsOn = true)
        assertEquals(ShiftMode.ONCE, auto.shift)
        assertEquals(ShiftMode.OFF, auto.tapShift(0).shift)
    }

    @Test
    fun `symbols are never uppercased`() {
        val state = KeyboardState(layer = Layer.SYMBOLS, shift = ShiftMode.LOCKED)
        assertEquals("%", state.textFor(Key.Text("%")))
    }

    @Test
    fun `enter action follows ime options`() {
        assertEquals(EditorInfo.IME_ACTION_SEND, enterAction(EditorInfo.IME_ACTION_SEND))
        assertNull(enterAction(EditorInfo.IME_ACTION_UNSPECIFIED))
        assertNull(enterAction(EditorInfo.IME_ACTION_NONE))
        assertNull(enterAction(EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_ENTER_ACTION))
    }

    @Test
    fun `number and phone fields open on symbols`() {
        assertEquals(Layer.SYMBOLS, initialLayer(InputType.TYPE_CLASS_NUMBER))
        assertEquals(Layer.SYMBOLS, initialLayer(InputType.TYPE_CLASS_PHONE))
        assertEquals(Layer.LETTERS, initialLayer(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))
    }

    @Test
    fun `every row fills exactly the row width or less`() {
        for (layer in Layer.entries) {
            keyboardRows(layer).forEach { row ->
                val width = row.sumOf { keyWeight(it, row).toDouble() }
                assert(width <= ROW_UNITS + 1e-6) { "$layer row $row is $width units" }
            }
            // A space bar stretches its row to the full width.
            val bottom = keyboardRows(layer).last()
            if (Key.Space in bottom) {
                assertEquals(ROW_UNITS.toDouble(), bottom.sumOf { keyWeight(it, bottom).toDouble() }, 1e-6)
            }
        }
    }

    @Test
    fun `shift keeps letters whose capital is not one character`() {
        val shifted = KeyboardState(shift = ShiftMode.ONCE)
        assertEquals("ß", shifted.textFor(Key.Text("ß")))
        assertEquals("À", shifted.textFor(Key.Text("à")))
    }

    @Test
    fun `top row hints are digits, other keys have none`() {
        assertEquals("1", hintFor(Key.Text("q"), Layer.LETTERS))
        assertEquals("0", hintFor(Key.Text("p"), Layer.LETTERS))
        assertNull(hintFor(Key.Text("a"), Layer.LETTERS))
        assertNull(hintFor(Key.Text("1"), Layer.SYMBOLS))
        assertNull(hintFor(Key.Space, Layer.LETTERS))
    }

    @Test
    fun `alternates row starts at the key and selects by finger position`() {
        val layout = altLayout(keyLeft = 100f, keyRight = 160f, cellWidth = 60f, count = 3, maxWidth = 720f)
        assertEquals(AltLayout(100f, reversed = false), layout)
        assertEquals(0, altIndexAt(130f, layout, 60f, 3))
        assertEquals(2, altIndexAt(250f, layout, 60f, 3))
        assertEquals(2, altIndexAt(5000f, layout, 60f, 3))
        assertEquals(0, altIndexAt(-50f, layout, 60f, 3))
    }

    @Test
    fun `alternates row flips at the right edge so the first option stays above the key`() {
        val layout = altLayout(keyLeft = 580f, keyRight = 640f, cellWidth = 60f, count = 5, maxWidth = 720f)
        assertEquals(AltLayout(340f, reversed = true), layout)
        assertEquals(0, altIndexAt(610f, layout, 60f, 5))
        assertEquals(4, altIndexAt(350f, layout, 60f, 5))
    }

    @Test
    fun `every alternate is a single character`() {
        for (layer in Layer.entries) for (key in keyboardRows(layer).flatten()) {
            alternatesFor(key).forEach { assertEquals("$key -> $it", 1, it.codePointCount(0, it.length)) }
        }
    }

    @Test
    fun `comma sits left of the emoji key on every typing layer`() {
        for (layer in listOf(Layer.LETTERS, Layer.SYMBOLS, Layer.MORE_SYMBOLS)) {
            val bottom = keyboardRows(layer).last()
            assertEquals(bottom.indexOf(Key.Emoji) - 1, bottom.indexOf(Key.Text(",")))
        }
    }

    @Test
    fun `symbols pages link to each other`() {
        assert(Key.ToMoreSymbols in keyboardRows(Layer.SYMBOLS).flatten())
        assert(Key.ToSymbols in keyboardRows(Layer.MORE_SYMBOLS).flatten())
        assertEquals(Key.ToLetters, keyboardRows(Layer.MORE_SYMBOLS).last().first())
    }

    @Test
    fun `emoji key replaces the globe, and the emoji layer keeps only ABC and delete`() {
        assert(Key.Emoji in keyboardRows(Layer.LETTERS).last())
        assert(Key.Emoji in keyboardRows(Layer.SYMBOLS).last())
        assertEquals(listOf(listOf(Key.ToLetters, Key.Backspace)), keyboardRows(Layer.EMOJI))
    }

    @Test
    fun `emoji asset format parses groups and skips comments`() {
        val parsed = parseEmoji("# header\nSmileys\t😀 😃\n\nFlags\t🏁\n")
        assertEquals(listOf("Smileys" to listOf("😀", "😃"), "Flags" to listOf("🏁")), parsed)
    }

    @Test
    fun `recents put the newest first without duplicates and cap the list`() {
        assertEquals(listOf("b", "a"), pushRecent(listOf("a"), "b"))
        assertEquals(listOf("a", "b"), pushRecent(listOf("b", "a"), "a"))
        assertEquals(3, pushRecent(listOf("a", "b", "c"), "d", max = 3).size)
    }

    @Test
    fun `suggestions and autocorrect follow the field type`() {
        val text = InputType.TYPE_CLASS_TEXT
        assert(suggestionsAllowed(text))
        assert(autocorrectAllowed(text))
        assert(!suggestionsAllowed(text or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assert(!suggestionsAllowed(text or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS))
        assert(!suggestionsAllowed(InputType.TYPE_CLASS_NUMBER))
        assert(suggestionsAllowed(text or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS))
        assert(!autocorrectAllowed(text or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS))
        assert(!autocorrectAllowed(text or InputType.TYPE_TEXT_VARIATION_URI))
    }

    @Test
    fun `word characters and word-ending punctuation`() {
        assert(isWordChar("a", ""))
        assert(!isWordChar("'", ""))
        assert(isWordChar("'", "don"))
        assert(!isWordChar("1", "a"))
        assert(endsWord("."))
        assert(endsWord("?"))
        assert(!endsWord("-"))
    }

    @Test
    fun `word delete removes the last word and the spaces after it`() {
        assertEquals(3, wordDeleteLength("hello wor"))
        assertEquals(6, wordDeleteLength("hello "))
        assertEquals(6, wordDeleteLength("hi you.  "))
        assertEquals(5, wordDeleteLength("a\nhello"))
        assertEquals(0, wordDeleteLength(""))
        assertEquals(3, wordDeleteLength("   "))
        // Cut off after the first half of "😀": that half stays.
        assertEquals(2, wordDeleteLength("\uDE00ab"))
    }

    @Test
    fun `cursor moves whole emoji`() {
        assertEquals(2, cursorDelta("abc", 2, forward = true))
        assertEquals(3, cursorDelta("abc", 9, forward = true))
        assertEquals(2, cursorDelta("😀a", 1, forward = true))
        assertEquals(2, cursorDelta("a😀", 1, forward = false))
        assertEquals(1, cursorDelta("😀a", 1, forward = false))
        assertEquals(0, cursorDelta("", 3, forward = false))
    }

    private val index = parseEmojiKeywords(
        "# header\n" +
            "😀\tgrinning face\thappy smile\n" +
            "❤️\tred heart\tlove\n" +
            "😍\tsmiling face with heart-eyes\tlove\n" +
            "🇯🇵\tflag: japan\n",
    )

    @Test
    fun `emoji search matches word prefixes in names and keywords`() {
        assertEquals(listOf("😀"), searchEmoji(index, "grin"))
        assertEquals(listOf("🇯🇵"), searchEmoji(index, "japan"))
        assertEquals(listOf("😀"), searchEmoji(index, "Happy Face"))
        assertEquals(emptyList<String>(), searchEmoji(index, "   "))
        assertEquals(emptyList<String>(), searchEmoji(index, "xyz"))
    }

    @Test
    fun `emoji search ranks name matches before keyword matches`() {
        // "heart" is in both names; "love" only in keywords.
        assertEquals(listOf("❤️", "😍"), searchEmoji(index, "heart"))
        assertEquals(listOf("😍"), searchEmoji(index, "smiling heart love"))
        assertEquals(listOf("❤️", "😍"), searchEmoji(index, "love"))
    }

    @Test
    fun `skin tones remember the last pick per emoji`() {
        val tones = EmojiTones(parseEmojiTones("# header\n👍\t👍🏻 👍🏼 👍🏽 👍🏾 👍🏿\n"))
        assertEquals(listOf("👍", "👍🏻", "👍🏼", "👍🏽", "👍🏾", "👍🏿"), tones.options("👍🏽"))
        assertEquals(emptyList<String>(), tones.options("😀"))
        assertEquals("👍", tones.shown("👍"))

        val picked = tones.withChoice("👍🏾")
        assertEquals("👍🏾", picked.shown("👍"))
        // Typing the same pick, or an emoji without tones, changes nothing.
        assert(picked.withChoice("👍🏾") === picked)
        assert(picked.withChoice("😀") === picked)
        // Picking the plain base goes back to no tone.
        assertEquals("👍", picked.withChoice("👍").shown("👍"))
    }

    @Test
    fun `sentences start at the field start, new lines and after end marks with a space`() {
        assertEquals(true, sentenceStart(""))
        assertEquals(true, sentenceStart("   "))
        assertEquals(true, sentenceStart("Hello.\n"))
        assertEquals(true, sentenceStart("Done. "))
        assertEquals(true, sentenceStart("Really?  "))
        assertEquals(true, sentenceStart("He said \"yes.\" "))
        assertEquals(false, sentenceStart("Done."))
        assertEquals(false, sentenceStart("e.g"))
        assertEquals(false, sentenceStart("hello "))
        assertEquals(false, sentenceStart("one, "))
    }

    @Test
    fun `no auto caps in passwords, emails or URLs`() {
        val text = InputType.TYPE_CLASS_TEXT
        assertEquals(true, autoCapsAllowed(text))
        assertEquals(false, autoCapsAllowed(text or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertEquals(false, autoCapsAllowed(text or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS))
        assertEquals(false, autoCapsAllowed(text or InputType.TYPE_TEXT_VARIATION_URI))
        assertEquals(false, autoCapsAllowed(InputType.TYPE_CLASS_NUMBER))
    }

    @Test
    fun `digits and punctuation join a full stop, letters do not`() {
        listOf("1", ".", ",", "?", ")", "\"").forEach { assertEquals(it, true, joinsFullStop(it)) }
        listOf("a", "B", "(", "@", "").forEach { assertEquals(it, false, joinsFullStop(it)) }
    }

    @Test
    fun `stored toolbar keeps toolbar order and skips unknown names`() {
        assertEquals(listOf(ToolbarAction.CLIPBOARD, ToolbarAction.PASTE), parseToolbar("PASTE,CLIPBOARD"))
        assertEquals(listOf(ToolbarAction.CUT), parseToolbar("CUT,TRANSLATE,SETTINGS"))
        assertEquals(emptyList<ToolbarAction>(), parseToolbar(""))
    }
}

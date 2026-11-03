package app.zenith.keyboard

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
            val bottom = keyboardRows(layer).last()
            assertEquals(ROW_UNITS.toDouble(), bottom.sumOf { keyWeight(it, bottom).toDouble() }, 1e-6)
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
}

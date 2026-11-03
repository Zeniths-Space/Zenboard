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
}

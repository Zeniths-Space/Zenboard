package com.elara.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {
    @Test fun unknownThemeNameFallsBackToElara() {
        assertEquals(KeyboardTheme.Brand, KeyboardTheme.parse("nonsense"))
        assertEquals(KeyboardTheme.Brand, KeyboardTheme.parse(null))
    }

    @Test fun everyThemeRoundTripsByName() {
        KeyboardTheme.entries.forEach { assertEquals(it, KeyboardTheme.parse(it.name)) }
    }
}

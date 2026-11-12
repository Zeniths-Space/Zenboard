package com.elara.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class LearnedWordsTest {
    @Test fun newestWordGoesLast() {
        assertEquals(listOf("a", "b", "c"), LearnedWords.withRecent(listOf("a", "b"), "c", 10))
    }

    @Test fun repeatedWordMovesToTheEnd() {
        assertEquals(listOf("b", "a"), LearnedWords.withRecent(listOf("a", "b"), "a", 10))
    }

    @Test fun oldestIsDroppedPastTheCap() {
        assertEquals(listOf("b", "c"), LearnedWords.withRecent(listOf("a", "b"), "c", 2))
    }
}

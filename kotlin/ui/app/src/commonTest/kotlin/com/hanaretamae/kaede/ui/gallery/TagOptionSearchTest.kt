package com.hanaretamae.kaede.ui.gallery

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TagOptionSearchTest {
    @Test
    fun matchesTextAndFuzzyTagWords() {
        assertTrue(tagOptionMatches("Source", "Illustration", "source/illustration", "illustraton"))
        assertTrue(tagOptionMatches("人数", "女性", "人数/女性", "女性"))
        assertFalse(tagOptionMatches("Source", "Art style", "source/art-style", "copyright"))
    }

    @Test
    fun searchIsBoundedByCodePointLength() {
        val longQuery = "x".repeat(256)
        assertFalse(tagOptionMatches("tag", "label", "tag/label", longQuery))
    }
}

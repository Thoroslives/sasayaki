package com.sasayaki.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class BubbleSizesTest {
    @Test
    fun `controls keep the size people chose before the split`() {
        assertEquals(BubbleSize.LARGER, controlsSizeFrom(controls = null, bubble = "LARGER"))
    }

    @Test
    fun `a chosen controls size wins over the bubble size`() {
        assertEquals(BubbleSize.LARGE, controlsSizeFrom(controls = "LARGE", bubble = "SMALL"))
    }

    @Test
    fun `a small bubble does not shrink the controls`() {
        assertEquals(BubbleSize.STANDARD, controlsSizeFrom(controls = null, bubble = "SMALL"))
    }

    @Test
    fun `unknown names fall back to standard`() {
        assertEquals(BubbleSize.STANDARD, controlsSizeFrom(controls = "GIANT", bubble = null))
    }
}

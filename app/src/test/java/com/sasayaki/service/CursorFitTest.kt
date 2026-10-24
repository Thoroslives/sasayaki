package com.sasayaki.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CursorFitTest {

    @Test
    fun `an unknown cursor leaves the dictation alone`() {
        assertEquals(" Hello there.", fitToCursor(null, " Hello there."))
    }

    @Test
    fun `the start of a field gets no leading space`() {
        assertEquals("Hello there.", fitToCursor("", " Hello there."))
    }

    @Test
    fun `a new sentence keeps its capital and gets a space`() {
        assertEquals(" See you soon.", fitToCursor("Thanks.", "See you soon."))
        assertEquals("See you soon.", fitToCursor("Thanks. ", " See you soon."))
        assertEquals("See you soon.", fitToCursor("Thanks\n", "See you soon."))
        assertEquals("See you soon.", fitToCursor("Thanks,\n  ", "See you soon."))
    }

    @Test
    fun `a continuation after a comma lowers common words only`() {
        assertEquals(" how are you?", fitToCursor("Hey,", "How are you?"))
        assertEquals("the rest is fine.", fitToCursor("Mostly done, ", " The rest is fine."))
        assertEquals(" Maria will call.", fitToCursor("Thanks,", "Maria will call."))
        assertEquals(" I think so.", fitToCursor("Well,", "I think so."))
        assertEquals(" Sie haben recht.", fitToCursor("Ja,", "Sie haben recht."))
        assertEquals(" die Lösung passt.", fitToCursor("Ich glaube,", "Die Lösung passt."))
    }

    @Test
    fun `text after a word keeps its capital`() {
        assertEquals(" See you tomorrow", fitToCursor("Sounds good", "See you tomorrow"))
        assertEquals(" \"Hello,\" she said.", fitToCursor("He said,", "\"Hello,\" she said."))
    }

    @Test
    fun `a dictated line break is kept as it came`() {
        assertEquals("\n\nThanks for the update.", fitToCursor("Hi John,", "\n\nThanks for the update."))
    }

    @Test
    fun `punctuation, brackets and handles stay tight`() {
        assertEquals(", right?", fitToCursor("Tomorrow", ", right?"))
        assertEquals("Maybe", fitToCursor("(", "Maybe"))
        assertEquals("Hola", fitToCursor("¿", "Hola"))
        assertEquals("john", fitToCursor("@", "john"))
        assertEquals("release", fitToCursor("#", "release"))
    }

    @Test
    fun `scripts written without spaces get none`() {
        assertEquals("明日行きます。", fitToCursor("了解しました。", "明日行きます。"))
        assertEquals("好的", fitToCursor("我们", "好的"))
    }

    @Test
    fun `straight quotes open after a space and close after a word`() {
        assertEquals("Hello", fitToCursor("He said \"", "Hello"))
        assertEquals(" And left.", fitToCursor("He said \"hi\"", "And left."))
    }

    @Test
    fun `surrounding text maps to the text before the cursor`() {
        assertEquals("Hey", textBeforeCursor("Hey", 3, 3))
        assertEquals("He", textBeforeCursor("Hey", 3, 2))
        assertNull(textBeforeCursor("Hey", -1, -1))
        assertNull(textBeforeCursor("Hey", 9, 9))
        assertNull(textBeforeCursor(null, 0, 0))
    }
}

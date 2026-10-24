package com.sasayaki.service

import org.junit.Assert.assertEquals
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
    fun `a continued sentence is lowered and spaced`() {
        assertEquals(" how are you?", fitToCursor("Hey", "How are you?"))
        assertEquals(" how are you?", fitToCursor("Hey,", "How are you?"))
        assertEquals("how are you?", fitToCursor("Hey, ", " How are you?"))
    }

    @Test
    fun `the pronoun I and acronyms keep their capitals`() {
        assertEquals(" I think so.", fitToCursor("Well,", "I think so."))
        assertEquals(" I'm in.", fitToCursor("Sure", "I'm in."))
        assertEquals(" NASA called.", fitToCursor("Guess what,", "NASA called."))
    }

    @Test
    fun `punctuation and brackets stay tight`() {
        assertEquals(", right?", fitToCursor("Tomorrow", ", right?"))
        assertEquals("Maybe", fitToCursor("(", "Maybe"))
        assertEquals("Hola", fitToCursor("¿", "Hola"))
    }

    @Test
    fun `straight quotes open after a space and close after a word`() {
        assertEquals("Hello", fitToCursor("He said \"", "Hello"))
        assertEquals(" And left.", fitToCursor("He said \"hi\"", "And left."))
    }
}

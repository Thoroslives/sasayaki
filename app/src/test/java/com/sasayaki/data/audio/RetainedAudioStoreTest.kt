package com.sasayaki.data.audio

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RetainedAudioStoreTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val directory by lazy { File(temp.root, "retained_audio") }
    private val store by lazy { RetainedAudioStore(directory) }

    @Test
    fun `retain copies a recording into the store`() = runTest {
        val recording = temp.newFile("recording.wav").apply { writeText("audio") }

        val path = store.retain(recording)!!

        assertEquals(directory.canonicalFile, File(path).canonicalFile.parentFile)
        assertEquals("audio", File(path).readText())
    }

    @Test
    fun `retain reuses audio already in the store`() = runTest {
        val first = store.retain(temp.newFile("recording.wav"))!!

        assertEquals(first, store.retain(File(first)))
        assertEquals(1, directory.listFiles()!!.size)
    }

    @Test
    fun `delete leaves files outside the store alone`() = runTest {
        val lookalike = File(temp.root, "retained_audio_other").apply { mkdirs() }
        val outside = File(lookalike, "dictation.wav").apply { writeText("keep") }

        store.delete(outside.path)
        store.delete(File(directory, "../retained_audio_other/dictation.wav").path)

        assertTrue(outside.exists())
    }

    @Test
    fun `deleteAllExcept removes only unreferenced audio`() = runTest {
        val kept = store.retain(temp.newFile("a.wav"))!!
        val orphan = store.retain(temp.newFile("b.wav"))!!

        store.deleteAllExcept(setOf(kept))

        assertTrue(File(kept).exists())
        assertFalse(File(orphan).exists())
    }
}

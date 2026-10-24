package com.sasayaki.service

import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AudioRecorderTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `a stop that lands before recording starts keeps the microphone closed`() = runTest {
        val recorder = AudioRecorder()
        val output = File(temp.root, "recording.pcm")

        recorder.stop()
        recorder.record(output)

        assertFalse(output.exists())
    }
}

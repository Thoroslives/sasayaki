package com.sasayaki.domain.transcription

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class SpeechDetectorTest {
    private val rate = 16_000

    private fun pcm(samples: List<Double>): ByteArrayInputStream {
        val buffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { buffer.putShort((it.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()) }
        return ByteArrayInputStream(buffer.array())
    }

    private fun noise(seconds: Double, amplitude: Double, random: Random = Random(7)) =
        List((seconds * rate).toInt()) { (random.nextDouble() * 2 - 1) * amplitude }

    // Voiced syllables: a 180 Hz tone at speaking level, switched on and off like words.
    private fun speech(seconds: Double, amplitude: Double = 0.08) =
        List((seconds * rate).toInt()) { i ->
            val t = i.toDouble() / rate
            val wordOn = (t * 4).toInt() % 2 == 0
            if (wordOn) sin(2 * PI * 180 * t) * amplitude else 0.0
        }

    private fun detects(samples: List<Double>) = SpeechDetector.containsSpeech(SpeechDetector.frameLevels(pcm(samples), rate))

    @Test
    fun `digital silence is not speech`() {
        assertFalse(detects(List(rate * 2) { 0.0 }))
    }

    @Test
    fun `quiet room noise is not speech`() {
        assertFalse(detects(noise(seconds = 3.0, amplitude = 0.002)))
    }

    @Test
    fun `steady loud hum is not speech`() {
        assertFalse(detects(List(rate * 3) { i -> sin(2 * PI * 120 * i / rate) * 0.1 }))
    }

    @Test
    fun `words over background noise are speech`() {
        val samples = noise(1.0, 0.002) + speech(1.5).zip(noise(1.5, 0.002)) { a, b -> a + b } + noise(1.0, 0.002)
        assertTrue(detects(samples))
    }

    @Test
    fun `a short word right away still counts`() {
        assertTrue(detects(speech(0.6) + List(rate / 2) { 0.0 }))
    }

    @Test
    fun `a click is too short to be speech`() {
        val samples = List(rate * 2) { 0.0 }.toMutableList()
        repeat(rate / 50) { samples[rate + it] = 0.5 }
        assertFalse(detects(samples))
    }
}

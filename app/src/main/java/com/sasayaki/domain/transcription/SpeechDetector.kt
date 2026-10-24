package com.sasayaki.domain.transcription

import java.io.InputStream
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * SpeechDetector decides locally whether a recording holds anything worth transcribing, so
 * silence and room noise are never sent to a server, where Whisper-family models tend to
 * hallucinate text ("Thank you.") out of nothing.
 */
object SpeechDetector {
    private const val FRAME_MS = 30
    private const val SILENT_DB = -100f
    // Below this a frame is quiet whatever the room sounds like. Thresholds lean
    // permissive: dropping a whispered dictation is worse than sending a quiet one.
    private const val ABSOLUTE_FLOOR_DB = -55f
    // Speech must stand this far above the recording's own quiet frames.
    private const val ABOVE_NOISE_DB = 8f
    // Steady signals (silence, hum, a fan) vary less than this between quiet and loud frames.
    private const val MIN_DYNAMIC_RANGE_DB = 6f
    private const val MIN_SPEECH_MS = 200
    // The start haptic and the tap on stop sit at the edges of every manual recording and
    // are loud enough to pass for speech, so the edges are ignored.
    private const val IGNORED_START_MS = 300
    private const val IGNORED_END_MS = 250
    // Syllables last longer than clicks, taps and bumps; only runs this long count.
    private const val MIN_RUN_MS = 90

    /** frameLevels reads 16-bit little-endian mono PCM and returns each 30 ms frame's level in dBFS. */
    fun frameLevels(pcm: InputStream, sampleRate: Int): FloatArray {
        val frameSamples = sampleRate * FRAME_MS / 1000
        val bytes = ByteArray(frameSamples * 2)
        val levels = ArrayList<Float>()
        while (true) {
            val read = pcm.readFully(bytes)
            if (read < 2) break
            val samples = read / 2
            var sum = 0.0
            for (i in 0 until samples) {
                val sample = ((bytes[i * 2 + 1].toInt() shl 8) or (bytes[i * 2].toInt() and 0xFF)).toShort()
                sum += sample.toDouble() * sample
            }
            val rms = sqrt(sum / samples)
            levels += if (rms < 1.0) SILENT_DB else (20 * log10(rms / Short.MAX_VALUE)).toFloat()
        }
        return levels.toFloatArray()
    }

    private fun InputStream.readFully(buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val read = read(buffer, total, buffer.size - total)
            if (read < 0) break
            total += read
        }
        return total
    }

    fun containsSpeech(levels: FloatArray): Boolean {
        val start = IGNORED_START_MS / FRAME_MS
        val end = levels.size - IGNORED_END_MS / FRAME_MS
        if (end - start <= 0) return false
        val window = levels.copyOfRange(start, end)
        val sorted = window.sorted()
        val noiseFloor = sorted[sorted.size / 10]
        val loud = sorted[sorted.size * 9 / 10]
        if (loud - noiseFloor < MIN_DYNAMIC_RANGE_DB) return false
        val threshold = maxOf(ABSOLUTE_FLOOR_DB, noiseFloor + ABOVE_NOISE_DB)
        return sustainedFrames(window, threshold) * FRAME_MS >= MIN_SPEECH_MS
    }

    private fun sustainedFrames(levels: FloatArray, threshold: Float): Int {
        val minRun = MIN_RUN_MS / FRAME_MS
        var total = 0
        var run = 0
        for (level in levels) {
            if (level > threshold) {
                run++
            } else {
                if (run >= minRun) total += run
                run = 0
            }
        }
        if (run >= minRun) total += run
        return total
    }
}

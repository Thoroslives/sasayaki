package com.sasayaki.service

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class AudioRecorder {
    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    // stopRequested is never reset: an AudioRecorder records once, so a stop() that lands
    // before record() has opened the microphone still ends the recording.
    @Volatile
    private var stopRequested = false

    @Volatile
    private var isPaused = false
    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    val bufferSize: Int by lazy {
        AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(4096)
    }

    @SuppressLint("MissingPermission")
    suspend fun record(outputFile: File): Unit = withContext(Dispatchers.IO) {
        if (stopRequested) return@withContext
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            Log.e(TAG, "AudioRecord failed to initialize")
            return@withContext
        }

        val buffer = ShortArray(bufferSize / 2)

        try {
            recorder.startRecording()

            FileOutputStream(outputFile).use { fos ->
                while (!stopRequested && isActive) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        if (isPaused) {
                            _audioLevel.value = 0f
                            continue
                        }
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += buffer[i].toDouble() * buffer[i].toDouble()
                        }
                        val rms = sqrt(sum / read)
                        _audioLevel.value = (rms / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)

                        val byteBuffer = ByteArray(read * 2)
                        for (i in 0 until read) {
                            byteBuffer[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = (buffer[i].toInt() shr 8 and 0xFF).toByte()
                        }
                        fos.write(byteBuffer)
                    } else if (read < 0) {
                        Log.e(TAG, "AudioRecord.read() returned error: $read")
                        break
                    }
                }
            }
        } finally {
            try {
                recorder.stop()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recorder", e)
            }
            recorder.release()
            _audioLevel.value = 0f
        }
    }

    fun stop() {
        stopRequested = true
        isPaused = false
    }

    fun pause() {
        isPaused = true
        _audioLevel.value = 0f
    }

    fun resume() {
        isPaused = false
    }
}

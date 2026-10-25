package com.sasayaki.service.dictation

import android.content.Context
import com.sasayaki.domain.model.AppContext
import com.sasayaki.domain.model.Profile
import com.sasayaki.domain.transcription.AudioConverter
import com.sasayaki.domain.transcription.SpeechDetector
import com.sasayaki.domain.transcription.TranscriptionManager
import com.sasayaki.service.AudioRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

sealed interface ProcessedRecording {
    data object Empty : ProcessedRecording
    data object NoSpeech : ProcessedRecording

    /** Transcribed fails only with [com.sasayaki.domain.transcription.DictationFailure]. */
    data class Transcribed(val result: Result<String>) : ProcessedRecording
}

interface ProcessesRecordings {
    suspend fun process(
        pcm: File,
        durationMs: Long,
        appContext: AppContext?,
        profile: Profile?,
        onPostProcessing: () -> Unit,
        incognito: Boolean = false
    ): ProcessedRecording

    suspend fun retry(entryId: Long, onPostProcessing: () -> Unit): Result<String>
}

/**
 * RecordingProcessor turns a finished PCM recording into text: silent recordings are dropped
 * on the phone, the rest are sent for transcription and cleanup. The caller owns the PCM file.
 */
@Singleton
class RecordingProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transcriptionManager: TranscriptionManager
) : ProcessesRecordings {
    override suspend fun process(
        pcm: File,
        durationMs: Long,
        appContext: AppContext?,
        profile: Profile?,
        onPostProcessing: () -> Unit,
        incognito: Boolean
    ): ProcessedRecording {
        val hasAudio = withContext(Dispatchers.IO) { pcm.exists() && pcm.length() > 0L }
        if (!hasAudio) return ProcessedRecording.Empty

        val heardSpeech = withContext(Dispatchers.IO) {
            pcm.inputStream().buffered().use { stream ->
                SpeechDetector.containsSpeech(SpeechDetector.frameLevels(stream, AudioRecorder.SAMPLE_RATE))
            }
        }
        if (!heardSpeech) return ProcessedRecording.NoSpeech

        val wav = File(context.cacheDir, "recording_${System.currentTimeMillis()}.wav")
        return try {
            withContext(Dispatchers.IO) {
                AudioConverter.pcmToWav(pcm, wav)
                ProcessedRecording.Transcribed(
                    transcriptionManager.transcribe(wav, durationMs, appContext, profile, onPostProcessing, incognito)
                )
            }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { wav.delete() }
        }
    }

    override suspend fun retry(entryId: Long, onPostProcessing: () -> Unit): Result<String> =
        withContext(Dispatchers.IO) { transcriptionManager.retry(entryId, onPostProcessing) }
}

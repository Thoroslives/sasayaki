package com.sasayaki.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.sasayaki.data.preferences.BubbleSize
import com.sasayaki.data.preferences.PreferencesDataStore
import com.sasayaki.data.repository.ProfileRepository
import com.sasayaki.domain.model.AppContext
import com.sasayaki.domain.provider.describeConnectionError
import com.sasayaki.domain.transcription.AudioConverter
import com.sasayaki.domain.transcription.SpeechDetector
import com.sasayaki.domain.transcription.TranscriptionManager
import com.sasayaki.service.bubble.BubbleActions
import com.sasayaki.service.bubble.BubbleOverlay
import com.sasayaki.service.bubble.BubbleUiState
import com.sasayaki.service.bubble.WAVEFORM_BARS
import com.sasayaki.service.bubble.toPhase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class BubbleService : Service() {
    companion object {
        private const val TAG = "BubbleService"
        private const val INSERTED_FLASH_MS = 700L
        private const val NO_SPEECH_NOTICE_MS = 2500L
        const val ACTION_START = "com.sasayaki.ACTION_START_BUBBLE"

        @Volatile
        var isRunning: Boolean = false
            private set(value) {
                field = value
                _runningState.value = value
            }

        private val _runningState = MutableStateFlow(false)
        val runningState: StateFlow<Boolean> = _runningState.asStateFlow()

        fun start(context: Context) {
            context.startForegroundService(Intent(context, BubbleService::class.java).apply {
                action = ACTION_START
            })
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BubbleService::class.java))
        }
    }

    @Inject lateinit var transcriptionManager: TranscriptionManager
    @Inject lateinit var textInjectionBridge: TextInjectionBridge
    @Inject lateinit var preferencesDataStore: PreferencesDataStore
    @Inject lateinit var profileRepository: ProfileRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var overlay: BubbleOverlay? = null
    private val uiState = MutableStateFlow(BubbleUiState())
    private val levels = MutableStateFlow<List<Float>>(emptyList())
    private var notificationHelper: NotificationHelper? = null
    private var hapticFeedback: HapticFeedback? = null
    private var audioRecorder: AudioRecorder? = null
    private var silenceDetector: SilenceDetector? = null
    @Volatile private var recordingJob: Job? = null
    private var transcriptionJob: Job? = null
    private var silenceCheckJob: Job? = null
    private var levelJob: Job? = null
    private var timerJob: Job? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var profileJob: Job? = null

    private var state: ServiceState = ServiceState.Idle
    private var recordingStartTime: Long = 0
    private var pausedStartedAt: Long = 0
    private var totalPausedMs: Long = 0
    private var recordingAppContext: AppContext? = null
    @Volatile private var pcmFile: File? = null

    // Keyboard-aware visibility
    private val keyboardListener = object : TextInjectorService.KeyboardListener {
        override fun onKeyboardVisibilityChanged(visible: Boolean) {
            scope.launch(Dispatchers.Main) {
                // An active dictation keeps the bubble up even if the keyboard closes.
                if (visible) overlay?.show() else if (state is ServiceState.Idle) overlay?.hide()
            }
        }
    }

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        BubbleTileService.requestRefresh(this)
        val helper = NotificationHelper(this)
        notificationHelper = helper
        hapticFeedback = HapticFeedback(this)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        helper.createNotificationChannel()
        profileJob = scope.launch {
            profileRepository.activeProfile.collect { profile ->
                uiState.update { it.copy(profileName = profile?.name.orEmpty()) }
            }
        }

        registerReceiver(stopReceiver, IntentFilter(NotificationHelper.ACTION_STOP), RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notificationHelper?.buildForegroundNotification() ?: run {
            Log.e(TAG, "NotificationHelper not initialized")
            stopSelf()
            return START_NOT_STICKY
        }
        // Idle as specialUse so the service can also be started from the boot receiver;
        // promoteToMicrophoneService() upgrades the type when recording actually starts.
        startForeground(
            NotificationHelper.NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission required.", Toast.LENGTH_LONG).show()
            stopSelf()
            return START_NOT_STICKY
        }

        prepareOverlay()

        // Register for keyboard events
        TextInjectorService.keyboardListener = keyboardListener

        // If keyboard is already visible, show immediately
        if (TextInjectorService.isKeyboardVisible) overlay?.show()

        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        BubbleTileService.requestRefresh(this)
        stopRecordingAndWait()
        overlay?.destroy()
        overlay = null
        TextInjectorService.keyboardListener = null
        try { unregisterReceiver(stopReceiver) } catch (_: Exception) {}
        scope.cancel()
        super.onDestroy()
    }

    private fun prepareOverlay() {
        if (overlay != null) return
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlay = BubbleOverlay(
            context = this,
            windowManager = windowManager,
            scope = scope,
            uiState = uiState,
            levels = levels,
            profiles = profileRepository.profiles.stateIn(scope, SharingStarted.Eagerly, emptyList()),
            wallpaperColors = preferencesDataStore.preferences
                .map { it.wallpaperColors }
                .stateIn(scope, SharingStarted.Eagerly, false),
            bubbleScale = preferencesDataStore.preferences
                .map { it.bubbleSize.scale }
                .stateIn(scope, SharingStarted.Eagerly, BubbleSize.STANDARD.scale),
            actions = BubbleActions(
                onStart = ::onBubbleTap,
                onStop = ::stopRecordingAndTranscribe,
                onCancel = ::cancelRecording,
                onTogglePause = ::toggleRecordingPause,
                onCancelProcessing = ::cancelProcessing,
                onRetry = ::retryBubbleError,
                onDismissError = ::cancelBubbleError
            ),
            onSelectProfile = { id ->
                hapticFeedback?.tick()
                scope.launch { profileRepository.activate(id) }
            },
            onRemove = ::stopSelf
        ).also { it.create() }
    }

    private fun onBubbleTap() {
        when (val current = state) {
            is ServiceState.Idle, ServiceState.NoSpeech -> startRecording()
            is ServiceState.Recording -> stopRecordingAndTranscribe()
            is ServiceState.Transcribing, is ServiceState.PostProcessing -> cancelProcessing()
            // Injection is already underway and cannot be meaningfully undone.
            is ServiceState.Injecting -> {}
            is ServiceState.Error -> current.retryEntryId?.let(::retrySavedFailure) ?: updateState(ServiceState.Idle)
        }
    }

    /**
     * Adds the microphone type to the running foreground service for the duration of a
     * recording. The service idles as specialUse so it can be started at boot, but the
     * platform requires the microphone type to be active while the mic is in use.
     *
     * @return false if the platform refused the promotion, in which case recording must
     *   not start — the user is told rather than left with a bubble that does nothing.
     */
    private fun promoteToMicrophoneService(): Boolean {
        val notification = notificationHelper?.buildForegroundNotification() ?: return false
        return try {
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not switch to microphone foreground service", e)
            showError("Microphone unavailable. Reopen Sasayaki and try again.", null)
            false
        }
    }

    /**
     * Drops back to specialUse once the microphone is released, so the system's
     * microphone-in-use indicator does not stay lit while the bubble sits idle.
     */
    private fun demoteToIdleService() {
        val notification = notificationHelper?.buildForegroundNotification() ?: return
        try {
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not drop microphone foreground service type", e)
        }
    }

    private fun startRecording() {
        if (!promoteToMicrophoneService()) return

        scope.launch {
            val prefs = preferencesDataStore.preferences.first()
            if (prefs.pauseOtherAudio) requestRecordingAudioFocus()
            if (prefs.vibrateOnRecord) hapticFeedback?.recordStart()
        }

        uiState.update { it.copy(elapsedSeconds = 0) }
        updateState(ServiceState.Recording())
        recordingStartTime = System.currentTimeMillis()
        pausedStartedAt = 0
        totalPausedMs = 0
        recordingAppContext = textInjectionBridge.currentAppContext

        audioRecorder = AudioRecorder()
        pcmFile = File(cacheDir, "recording_${System.currentTimeMillis()}.pcm")

        val recorder = audioRecorder ?: return
        val outputFile = pcmFile ?: return

        recordingJob = scope.launch(Dispatchers.IO) {
            try {
                recorder.record(outputFile)
            } catch (e: Exception) {
                Log.e(TAG, "Recording error", e)
            }
        }

        levels.value = emptyList()
        levelJob = scope.launch {
            recorder.audioLevel.collect { level ->
                levels.update { (it + level).takeLast(WAVEFORM_BARS) }
            }
        }

        timerJob = scope.launch {
            while (state is ServiceState.Recording) {
                uiState.update { it.copy(elapsedSeconds = recordedDurationMs() / 1000) }
                delay(250)
            }
        }

        silenceCheckJob = scope.launch {
            val prefs = preferencesDataStore.preferences.first()
            silenceDetector = SilenceDetector(
                audioLevel = recorder.audioLevel,
                silenceThresholdMs = prefs.silenceThresholdMs
            )
            delay(1000) // grace period
            while (state is ServiceState.Recording) {
                if ((state as? ServiceState.Recording)?.paused == true) {
                    delay(100)
                    continue
                }
                if (silenceDetector?.checkSilence() == true) {
                    stopRecordingAndTranscribe()
                    break
                }
                delay(100)
            }
        }
    }

    private fun toggleRecordingPause() {
        val current = state as? ServiceState.Recording ?: return
        if (current.paused) {
            totalPausedMs += System.currentTimeMillis() - pausedStartedAt
            pausedStartedAt = 0
            silenceDetector?.reset()
            audioRecorder?.resume()
            updateState(ServiceState.Recording(paused = false))
        } else {
            pausedStartedAt = System.currentTimeMillis()
            silenceDetector?.reset()
            audioRecorder?.pause()
            updateState(ServiceState.Recording(paused = true))
        }
    }

    /** Stop recording without transcribing (for cleanup on destroy) */
    private fun stopRecordingAndWait() {
        if (state !is ServiceState.Recording) return
        audioRecorder?.stop()
        levelJob?.cancel()
        timerJob?.cancel()
        silenceCheckJob?.cancel()
        // Don't cancel recordingJob — let it finish flushing the file
    }

    /** Stop recording and start transcription */
    private fun stopRecordingAndTranscribe() {
        if (state !is ServiceState.Recording) return

        // Signal the recorder to stop — do NOT cancel the job
        audioRecorder?.stop()
        demoteToIdleService()
        silenceCheckJob?.cancel()
        levelJob?.cancel()
        timerJob?.cancel()
        overlay?.dismissProfileMenu()

        scope.launch {
            val prefs = preferencesDataStore.preferences.first()
            if (prefs.vibrateOnRecord) hapticFeedback?.recordStop()
        }

        val durationMs = recordedDurationMs()
        updateState(ServiceState.Transcribing)

        transcriptionJob = scope.launch {
            var currentPcmFile: File? = null
            var wavFile: File? = null
            try {
                recordingJob?.join()

                currentPcmFile = pcmFile
                if (currentPcmFile == null || !currentPcmFile.exists() || currentPcmFile.length() == 0L) {
                    Log.w(TAG, "PCM file empty or missing")
                    updateState(ServiceState.Idle)
                    return@launch
                }

                val heardSpeech = withContext(Dispatchers.IO) {
                    currentPcmFile.inputStream().buffered().use { pcm ->
                        SpeechDetector.containsSpeech(SpeechDetector.frameLevels(pcm, AudioRecorder.SAMPLE_RATE))
                    }
                }
                if (!heardSpeech) {
                    showNoSpeech()
                    return@launch
                }

                wavFile = File(cacheDir, "recording_${System.currentTimeMillis()}.wav")
                withContext(Dispatchers.IO) {
                    AudioConverter.pcmToWav(currentPcmFile, wavFile)
                }

                val appContext = recordingAppContext

                val result = withContext(Dispatchers.IO) {
                    transcriptionManager.transcribe(wavFile, durationMs, appContext) {
                        scope.launch { updateState(ServiceState.PostProcessing) }
                    }
                }

                result.onSuccess { text ->
                    if (text.isNotBlank()) {
                        updateState(ServiceState.Injecting)
                        if (textInjectionBridge.inject(text)) flashInserted()
                        hapticFeedback?.complete()
                    }
                    updateState(ServiceState.Idle)
                }.onFailure { error ->
                    Log.e(TAG, "Transcription failed", error)
                    val retryEntryId = withContext(Dispatchers.IO) {
                        transcriptionManager.latestRetriableFailureId()
                    }
                    showError(describeConnectionError(error), retryEntryId)
                }
            } catch (e: CancellationException) {
                // Cancelled by the user; cancelProcessing() owns the state transition.
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Processing failed", e)
                val retryEntryId = withContext(Dispatchers.IO) {
                    transcriptionManager.latestRetriableFailureId()
                }
                showError(describeConnectionError(e), retryEntryId)
            } finally {
                abandonRecordingAudioFocus()
                currentPcmFile?.delete()
                wavFile?.delete()
                recordingJob = null
                pcmFile = null
                audioRecorder = null
                silenceDetector = null
            }
        }
    }

    /**
     * Aborts an in-flight transcription or post-processing request. The ASR and LLM calls
     * are suspending Retrofit calls, so cancelling the job cancels the HTTP request; the
     * coroutine's finally block still runs to release audio focus and temp files.
     */
    private fun cancelProcessing() {
        if (state !is ServiceState.Transcribing && state !is ServiceState.PostProcessing) return
        Log.d(TAG, "Cancelling processing at state=$state")
        transcriptionJob?.cancel()
        transcriptionJob = null
        scope.launch {
            val prefs = preferencesDataStore.preferences.first()
            if (prefs.vibrateOnRecord) hapticFeedback?.recordStop()
        }
        updateState(ServiceState.Idle)
    }

    private fun cancelRecording() {
        if (state !is ServiceState.Recording) return

        audioRecorder?.stop()
        demoteToIdleService()
        recordingJob?.cancel()
        silenceCheckJob?.cancel()
        levelJob?.cancel()
        timerJob?.cancel()
        overlay?.dismissProfileMenu()

        scope.launch {
            val prefs = preferencesDataStore.preferences.first()
            if (prefs.vibrateOnRecord) hapticFeedback?.recordStop()
            abandonRecordingAudioFocus()
        }

        updateState(ServiceState.Idle)

        scope.launch(Dispatchers.IO) {
            try {
                recordingJob?.join()
            } catch (e: Exception) {
                Log.w(TAG, "Recording cleanup interrupted", e)
            } finally {
                pcmFile?.delete()
                recordingJob = null
                pcmFile = null
                audioRecorder = null
                silenceDetector = null
            }
        }
    }

    private fun recordedDurationMs(): Long {
        val now = System.currentTimeMillis()
        val currentPauseMs = if ((state as? ServiceState.Recording)?.paused == true && pausedStartedAt > 0) {
            now - pausedStartedAt
        } else {
            0L
        }
        return (now - recordingStartTime - totalPausedMs - currentPauseMs).coerceAtLeast(0L)
    }

    private fun retrySavedFailure(entryId: Long) {
        updateState(ServiceState.Transcribing)
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                transcriptionManager.retry(entryId)
            }
            result.onSuccess { text ->
                if (text.isNotBlank()) {
                    updateState(ServiceState.Injecting)
                    if (textInjectionBridge.inject(text)) flashInserted()
                    hapticFeedback?.complete()
                }
                updateState(ServiceState.Idle)
            }.onFailure { error ->
                showError(describeConnectionError(error), entryId)
            }
        }
    }

    private fun retryBubbleError() {
        val retryEntryId = (state as? ServiceState.Error)?.retryEntryId
        if (retryEntryId != null) {
            retrySavedFailure(retryEntryId)
        } else {
            updateState(ServiceState.Idle)
        }
    }

    private fun cancelBubbleError() {
        updateState(ServiceState.Idle)
    }

    private fun showNoSpeech() {
        updateState(ServiceState.NoSpeech)
        scope.launch {
            delay(NO_SPEECH_NOTICE_MS)
            if (state is ServiceState.NoSpeech) updateState(ServiceState.Idle)
        }
    }

    private fun showError(message: String, retryEntryId: Long? = null) {
        updateState(ServiceState.Error(message, retryEntryId))
        hapticFeedback?.error()
        scope.launch {
            delay(if (retryEntryId == null) 4000 else 8000)
            if (state is ServiceState.Error) {
                updateState(ServiceState.Idle)
            }
        }
    }

    private fun flashInserted() {
        uiState.update { it.copy(justInserted = true) }
        scope.launch {
            delay(INSERTED_FLASH_MS)
            uiState.update { it.copy(justInserted = false) }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlay?.onDisplayChanged()
    }

    private fun updateState(newState: ServiceState) {
        state = newState
        uiState.update { it.copy(phase = newState.toPhase()) }
        if (newState is ServiceState.Idle && !TextInjectorService.isKeyboardVisible) overlay?.hide()
    }

    private fun requestRecordingAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
            audioFocusRequest = request
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    private fun abandonRecordingAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let(manager::abandonAudioFocusRequest)
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(null)
        }
    }
}

package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

object RealtimeVoiceManager : TextToSpeech.OnInitListener {
    private const val TAG = "RealtimeVoiceManager"
    private const val SAMPLE_RATE = 16000

    private var appContext: Context? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _micAmplitude = MutableStateFlow(0f)
    val micAmplitude: StateFlow<Float> = _micAmplitude.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            try {
                tts = TextToSpeech(appContext, this)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize TextToSpeech: ${e.message}")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            try {
                val result = tts?.setLanguage(Locale("hi", "IN"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
            } catch (e: Exception) {
                tts?.setLanguage(Locale.US)
            }
            Log.d(TAG, "TTS initialized successfully.")
        } else {
            Log.e(TAG, "TTS Initialization failed.")
        }
    }

    fun startMicRecording(context: Context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO permission not granted!")
            return
        }

        stopMicRecording() // Stop any previous recording session

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minBufferSize <= 0) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2
            )

            audioRecord?.startRecording()

            recordingJob = scope.launch {
                val buffer = ShortArray(minBufferSize)
                while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        var sum = 0.0
                        for (i in 0 until readSize) {
                            val sample = buffer[i].toDouble()
                            sum += sample * sample
                        }
                        val rms = Math.sqrt(sum / readSize)
                        // Normalize RMS to 0.0f .. 1.0f range
                        val normalized = (rms / 4000.0).coerceIn(0.0, 1.0).toFloat()
                        _micAmplitude.value = normalized
                        _isSpeaking.value = normalized > 0.08f
                    }
                }
            }
            Log.d(TAG, "Live Microphone recording started.")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AudioRecord: ${e.message}")
        }
    }

    fun stopMicRecording() {
        try {
            recordingJob?.cancel()
            recordingJob = null

            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
            audioRecord = null

            _micAmplitude.value = 0f
            _isSpeaking.value = false
            Log.d(TAG, "Microphone recording stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord: ${e.message}")
        }
    }

    fun speakAnnouncer(text: String, isVoiceEnabled: Boolean) {
        if (!isVoiceEnabled || !isTtsInitialized || tts == null) return
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "LudoAnnouncer_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text: ${e.message}")
        }
    }

    fun stopAnnouncer() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS: ${e.message}")
        }
    }
}

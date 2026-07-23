package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.Executors

enum class WaveType {
    SINE, SQUARE, TRIANGLE
}

object LudoAudioEngine {
    private const val TAG = "LudoAudioEngine"
    private const val SAMPLE_RATE = 22050

    // Shared Coroutine Scope for BGM and SFX
    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sfxExecutor = Executors.newSingleThreadExecutor()
    private val sfxDispatcher = sfxExecutor.asCoroutineDispatcher()

    private var bgmTrack: AudioTrack? = null
    private var bgmJob: Job? = null

    var isMusicEnabled: Boolean = true
        set(value) {
            field = value
            if (value) {
                startBgm()
            } else {
                stopBgm()
            }
        }

    var isSoundEnabled: Boolean = true

    var currentMusicMode: String = "GULF"
        set(value) {
            val changed = field != value
            field = value
            if (changed && isMusicEnabled && bgmJob != null && bgmJob?.isActive == true) {
                stopBgm()
                startBgm()
            }
        }

    fun startBgm() {
        if (!isMusicEnabled) return
        if (bgmJob != null && bgmJob?.isActive == true) return

        bgmJob = audioScope.launch {
            // Classic Upbeat Cheerful Ludo Theme Song Melody
            val classicMelody = listOf(
                Note(523.25, 200), Note(587.33, 200), Note(659.25, 200), Note(783.99, 400),
                Note(659.25, 200), Note(587.33, 200), Note(523.25, 400), Note(392.00, 400),
                Note(523.25, 200), Note(659.25, 200), Note(783.99, 200), Note(880.00, 400),
                Note(783.99, 200), Note(659.25, 200), Note(587.33, 400), Note(523.25, 400),
                Note(659.25, 200), Note(783.99, 200), Note(880.00, 200), Note(1046.50, 400),
                Note(880.00, 200), Note(783.99, 200), Note(659.25, 400), Note(587.33, 400),
                Note(523.25, 200), Note(587.33, 200), Note(659.25, 200), Note(783.99, 200),
                Note(880.00, 200), Note(783.99, 200), Note(659.25, 200), Note(523.25, 600)
            )

            // Gulf / Middle-Eastern Arabian Oud Hijaz Scale melody list
            val gulfMelody = listOf(
                Note(392.0, 250), Note(415.3, 250), Note(493.9, 500),
                Note(493.9, 250), Note(523.3, 250), Note(493.9, 250), Note(415.3, 250), Note(392.0, 500),
                Note(493.9, 250), Note(523.3, 250), Note(587.3, 500),
                Note(587.3, 250), Note(622.3, 250), Note(587.3, 250), Note(523.3, 250), Note(493.9, 500),
                Note(587.3, 250), Note(523.3, 250), Note(493.9, 250), Note(415.3, 250), Note(392.0, 600)
            )

            var noteIndex = 0

            // Resilient infinite BGM loop - automatically recovers track if system audio drops
            while (isActive && isMusicEnabled) {
                var currentTrack: AudioTrack? = null
                try {
                    val bufferSize = AudioTrack.getMinBufferSize(
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    ).coerceAtLeast(16384)

                    val track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()

                    bgmTrack = track
                    currentTrack = track
                    track.play()

                    while (isActive && isMusicEnabled) {
                        val melody = if (currentMusicMode == "GULF") gulfMelody else classicMelody
                        val note = melody[noteIndex % melody.size]
                        writeToneToTrack(track, note.frequency, note.durationMs, volume = 0.15f, type = WaveType.TRIANGLE)
                        noteIndex++
                        delay(50) // Small break between notes
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "BGM Error", e)
                    delay(300)
                } finally {
                    try {
                        currentTrack?.stop()
                        currentTrack?.release()
                    } catch (e: Exception) { /* ignore */ }
                    if (bgmTrack == currentTrack) {
                        bgmTrack = null
                    }
                }
            }
        }
    }

    fun stopBgm() {
        bgmJob?.cancel()
        bgmJob = null
        bgmTrack?.let {
            try {
                it.stop()
                it.release()
            } catch (e: Exception) { /* ignore */ }
        }
        bgmTrack = null
    }

    private fun writeToneToTrack(
        track: AudioTrack,
        frequency: Double,
        durationMs: Int,
        volume: Float,
        type: WaveType
    ) {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val samples = ShortArray(numSamples)

        val attackSamples = (numSamples * 0.15).toInt()
        val decaySamples = (numSamples * 0.85).toInt()

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val angle = 2.0 * Math.PI * frequency * t
            val waveVal = when (type) {
                WaveType.SINE -> Math.sin(angle)
                WaveType.SQUARE -> if (Math.sin(angle) >= 0) 1.0 else -1.0
                WaveType.TRIANGLE -> {
                    val x = angle / (2.0 * Math.PI)
                    2.0 * Math.abs(2.0 * (x - Math.floor(x + 0.5))) - 1.0
                }
            }

            // Envelope
            val env = when {
                i < attackSamples -> (i.toFloat() / attackSamples)
                else -> (1.0f - (i - attackSamples).toFloat() / decaySamples).coerceIn(0f, 1f)
            }

            samples[i] = (waveVal * Short.MAX_VALUE * volume * env).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        track.write(samples, 0, numSamples)
    }

    private fun playSequence(
        frequencies: List<Double>,
        durationsMs: List<Int>,
        volume: Float = 0.25f,
        type: WaveType = WaveType.TRIANGLE,
        gapMs: Long = 0
    ) {
        if (!isSoundEnabled) return
        audioScope.launch(sfxDispatcher) {
            var track: AudioTrack? = null
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.play()

                for (i in frequencies.indices) {
                    if (!isActive || !isSoundEnabled) break
                    val freq = frequencies[i]
                    val dur = durationsMs[i]
                    writeToneToTrack(track, freq, dur, volume, type)
                    if (gapMs > 0) {
                        delay(gapMs)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "SFX Error", e)
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (e: Exception) { /* ignore */ }
            }
        }
    }

    fun playTokenMove() {
        // Crisp, clear wooden token pick-up sound
        playSequence(listOf(750.0, 1050.0, 1350.0), listOf(20, 20, 25), volume = 0.40f, type = WaveType.SINE)
    }

    fun playTokenHop() {
        // High-clarity wooden token step clack sound on the board ("Tuk" sound)
        playSequence(listOf(1150.0, 1500.0), listOf(18, 22), volume = 0.45f, type = WaveType.SINE)
    }

    fun playTurnPass() {
        // Light double-blip
        playSequence(listOf(523.3, 659.3), listOf(50, 70), volume = 0.25f, type = WaveType.SINE)
    }

    fun playAlert() {
        // Alarm-like dual sound
        playSequence(listOf(880.0, 880.0, 880.0), listOf(80, 80, 80), volume = 0.30f, type = WaveType.SINE, gapMs = 40)
    }

    fun playDiceRoll() {
        // Highlighted, highly realistic, loud dice shaking & rolling sound ("Dish ki Awaj")
        playSequence(
            listOf(280.0, 720.0, 480.0, 920.0, 580.0, 1050.0, 410.0, 1250.0, 310.0, 1500.0, 220.0),
            listOf(18, 18, 18, 18, 18, 18, 18, 20, 20, 25, 75),
            volume = 0.70f,
            type = WaveType.TRIANGLE
        )
    }

    fun playTokenCaptured() {
        // High impact capture/cut sound
        val freqs = mutableListOf<Double>()
        val durs = mutableListOf<Int>()
        var f = 1200.0
        while (f >= 200.0) {
            freqs.add(f)
            durs.add(15)
            f -= 70.0
        }
        playSequence(freqs, durs, volume = 0.40f, type = WaveType.SQUARE)
    }

    fun playVictory() {
        // Fanfare
        playSequence(
            listOf(261.6, 329.6, 392.0, 523.3, 659.3, 784.0, 1046.5),
            listOf(70, 70, 70, 70, 70, 70, 400),
            volume = 0.22f,
            type = WaveType.SINE
        )
    }

    private data class Note(val frequency: Double, val durationMs: Int)
}

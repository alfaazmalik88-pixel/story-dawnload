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
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(16384)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
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
                track.play()

                // Classic Pentatonic melody list
                val classicMelody = listOf(
                    Note(261.6, 350), Note(293.7, 350), Note(329.6, 350), Note(392.0, 350),
                    Note(329.6, 350), Note(293.7, 350), Note(261.6, 700),
                    Note(329.6, 350), Note(392.0, 350), Note(440.0, 350), Note(523.3, 350),
                    Note(440.0, 350), Note(392.0, 350), Note(329.6, 700),
                    Note(392.0, 350), Note(440.0, 350), Note(523.3, 350), Note(587.3, 350),
                    Note(523.3, 350), Note(440.0, 350), Note(392.0, 700),
                    Note(440.0, 350), Note(392.0, 350), Note(329.6, 350), Note(293.7, 350),
                    Note(329.6, 350), Note(293.7, 350), Note(261.6, 700)
                )

                // Gulf / Middle-Eastern Arabian Oud Hijaz Scale melody list
                val gulfMelody = listOf(
                    Note(392.0, 350), Note(415.3, 350), Note(493.9, 700),
                    Note(493.9, 350), Note(523.3, 350), Note(493.9, 350), Note(415.3, 350), Note(392.0, 700),
                    Note(493.9, 350), Note(523.3, 350), Note(587.3, 700),
                    Note(587.3, 350), Note(622.3, 350), Note(587.3, 350), Note(523.3, 350), Note(493.9, 700),
                    Note(587.3, 350), Note(523.3, 350), Note(493.9, 350), Note(415.3, 350), Note(392.0, 700)
                )

                var noteIndex = 0
                while (isActive && isMusicEnabled) {
                    val melody = if (currentMusicMode == "GULF") gulfMelody else classicMelody
                    val note = melody[noteIndex % melody.size]
                    writeToneToTrack(track, note.frequency, note.durationMs, volume = 0.15f, type = WaveType.TRIANGLE)
                    noteIndex++
                    delay(50) // Small break between notes
                }
            } catch (e: Exception) {
                Log.e(TAG, "BGM Error", e)
            } finally {
                try {
                    bgmTrack?.stop()
                    bgmTrack?.release()
                } catch (e: Exception) { /* ignore */ }
                bgmTrack = null
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
                            .setUsage(AudioAttributes.USAGE_MEDIA)
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
        // A clean, beautiful introductory slide-click sound (SINE wave)
        playSequence(listOf(587.33, 880.0), listOf(30, 30), volume = 0.15f, type = WaveType.SINE)
    }

    fun playTokenHop() {
        // A short, extremely crisp, sweet, and clear retro step sound (SINE wave)
        playSequence(listOf(880.0, 1174.66), listOf(35, 35), volume = 0.18f, type = WaveType.SINE)
    }

    fun playTurnPass() {
        // Light double-blip
        playSequence(listOf(523.3, 659.3), listOf(50, 70), volume = 0.15f, type = WaveType.SINE)
    }

    fun playAlert() {
        // Alarm-like dual sound
        playSequence(listOf(880.0, 880.0, 880.0), listOf(80, 80, 80), volume = 0.15f, type = WaveType.SINE, gapMs = 40)
    }

    fun playDiceRoll() {
        // Fast rumbling sound
        playSequence(listOf(180.0, 240.0, 200.0, 310.0, 150.0), listOf(35, 35, 35, 35, 35), volume = 0.15f, type = WaveType.TRIANGLE)
    }

    fun playTokenCaptured() {
        // Downward slide sound
        val freqs = mutableListOf<Double>()
        val durs = mutableListOf<Int>()
        var f = 800.0
        while (f >= 150.0) {
            freqs.add(f)
            durs.add(15)
            f -= 50.0
        }
        playSequence(freqs, durs, volume = 0.25f, type = WaveType.SQUARE)
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

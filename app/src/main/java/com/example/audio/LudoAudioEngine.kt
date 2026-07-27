package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.Executors

enum class WaveType {
    SINE, SQUARE, TRIANGLE
}

object LudoAudioEngine {
    private const val TAG = "LudoAudioEngine"
    private const val SAMPLE_RATE = 44100

    // Dedicated Coroutine Scope & Single Thread Executor for ultra-low latency SFX
    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sfxExecutor = Executors.newSingleThreadExecutor()
    private val sfxDispatcher = sfxExecutor.asCoroutineDispatcher()

    private var mediaPlayer: MediaPlayer? = null
    private var bgmTrack: AudioTrack? = null
    private var sfxTrack: AudioTrack? = null
    private var bgmJob: Job? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

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

    @Synchronized
    private fun getSfxTrack(): AudioTrack? {
        if (!isSoundEnabled) return null
        if (sfxTrack == null) {
            try {
                val minBuff = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(16384)

                val track = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(minBuff)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.play()
                sfxTrack = track
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize persistent SFX AudioTrack", e)
                sfxTrack = null
            }
        } else {
            try {
                if (sfxTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    sfxTrack?.play()
                }
            } catch (e: Exception) { /* ignore */ }
        }
        return sfxTrack
    }

    fun startBgm(context: Context? = appContext) {
        if (context != null) {
            appContext = context.applicationContext
        }
        if (!isMusicEnabled) return
        if (bgmJob != null && bgmJob?.isActive == true) return

        bgmJob = audioScope.launch {
            val ctx = appContext
            var startedMp = false

            if (ctx != null) {
                try {
                    val rawNames = listOf("marketplace_at_noon", "marketplace", "bgm", "ludo_bgm", "music", "theme", "ludo_theme", "background")
                    var foundRawId = 0
                    for (name in rawNames) {
                        val id = ctx.resources.getIdentifier(name, "raw", ctx.packageName)
                        if (id != 0) {
                            foundRawId = id
                            break
                        }
                    }

                    if (foundRawId != 0) {
                        withContext(Dispatchers.Main) {
                            mediaPlayer?.release()
                            mediaPlayer = MediaPlayer.create(ctx, foundRawId)?.apply {
                                isLooping = true
                                setVolume(0.6f, 0.6f)
                                start()
                            }
                        }
                        if (mediaPlayer != null) {
                            startedMp = true
                        }
                    }

                    if (!startedMp) {
                        val assetNames = listOf("marketplace_at_noon.mp3", "marketplace.mp3", "bgm.mp3", "music.mp3", "ludo_bgm.mp3", "theme.mp3", "bgm.wav", "bgm.ogg")
                        for (assetName in assetNames) {
                            try {
                                val afd = ctx.assets.openFd(assetName)
                                withContext(Dispatchers.Main) {
                                    mediaPlayer?.release()
                                    mediaPlayer = MediaPlayer().apply {
                                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                        afd.close()
                                        isLooping = true
                                        setVolume(0.6f, 0.6f)
                                        prepare()
                                        start()
                                    }
                                }
                                if (mediaPlayer != null) {
                                    startedMp = true
                                    break
                                }
                            } catch (e: Exception) {
                                // asset not found, try next
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load raw/asset audio via MediaPlayer", e)
                }
            }

            if (startedMp) return@launch

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
                Note(392.0, 180), Note(415.3, 180), Note(493.9, 360),
                Note(493.9, 180), Note(523.3, 180), Note(493.9, 180), Note(415.3, 180), Note(392.0, 360),
                Note(392.0, 180), Note(415.3, 180), Note(493.9, 180), Note(523.3, 180), Note(587.3, 360),
                Note(587.3, 180), Note(622.3, 180), Note(587.3, 180), Note(523.3, 180), Note(493.9, 360),

                Note(493.9, 180), Note(523.3, 180), Note(587.3, 180), Note(622.3, 180), Note(739.99, 360),
                Note(783.99, 250), Note(739.99, 180), Note(622.3, 180), Note(587.3, 360),
                Note(587.3, 180), Note(523.3, 180), Note(493.9, 180), Note(415.3, 180), Note(392.0, 500),

                Note(392.0, 150), Note(493.9, 150), Note(587.3, 150), Note(783.99, 300),
                Note(739.99, 150), Note(622.3, 150), Note(587.3, 150), Note(523.3, 150), Note(493.9, 300),
                Note(415.3, 150), Note(493.9, 150), Note(523.3, 150), Note(493.9, 150), Note(415.3, 150), Note(392.0, 450),

                Note(392.0, 180), Note(587.3, 180), Note(392.0, 180), Note(587.3, 180),
                Note(493.9, 180), Note(523.3, 180), Note(415.3, 180), Note(392.0, 600)
            )

            var noteIndex = 0

            // Resilient infinite BGM loop with continuous audio streaming
            while (isActive && isMusicEnabled) {
                var currentTrack: AudioTrack? = null
                try {
                    val bufferSize = AudioTrack.getMinBufferSize(
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    ).coerceAtLeast(32768)

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
                        val isGulf = currentMusicMode == "GULF"
                        val melody = if (isGulf) gulfMelody else classicMelody
                        val note = melody[noteIndex % melody.size]

                        val notePcm = if (isGulf) {
                            generateOudToneBuffer(note.frequency, note.durationMs, volume = 0.28f)
                        } else {
                            generateToneBuffer(note.frequency, note.durationMs, volume = 0.18f, type = WaveType.TRIANGLE)
                        }

                        // Seamlessly write note PCM + small gap silence directly to AudioTrack (no coroutine delays)
                        track.write(notePcm, 0, notePcm.size)

                        val gapMs = if (isGulf) 25 else 45
                        val silenceBuffer = generateSilenceBuffer(gapMs)
                        track.write(silenceBuffer, 0, silenceBuffer.size)

                        noteIndex++
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
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) { /* ignore */ }
        mediaPlayer = null

        bgmTrack?.let {
            try {
                it.stop()
                it.release()
            } catch (e: Exception) { /* ignore */ }
            bgmTrack = null
        }
    }

    private fun generateToneBuffer(
        frequency: Double,
        durationMs: Int,
        volume: Float,
        type: WaveType
    ): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        if (numSamples <= 0) return ShortArray(0)
        val samples = ShortArray(numSamples)

        val attackSamples = (SAMPLE_RATE * 0.003).toInt().coerceAtLeast(1) // 3ms smooth attack
        val releaseSamples = (SAMPLE_RATE * 0.006).toInt().coerceAtLeast(1) // 6ms smooth release

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

            val env = when {
                i < attackSamples -> (i.toFloat() / attackSamples)
                i >= numSamples - releaseSamples -> ((numSamples - 1 - i).toFloat() / releaseSamples).coerceIn(0f, 1f)
                else -> 1.0f
            }

            samples[i] = (waveVal * 32767.0 * volume * env).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateOudToneBuffer(
        frequency: Double,
        durationMs: Int,
        volume: Float = 0.28f
    ): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        if (numSamples <= 0) return ShortArray(0)
        val samples = ShortArray(numSamples)

        val attackSamples = (SAMPLE_RATE * 0.008).toInt().coerceIn(1, numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val angle = 2.0 * Math.PI * frequency * t

            val fundamental = Math.sin(angle)
            val oct1 = 0.45 * Math.sin(2.0 * angle)
            val oct2 = 0.20 * Math.sin(3.0 * angle)
            val oct3 = 0.10 * Math.sin(4.0 * angle)

            val waveVal = (fundamental + oct1 + oct2 + oct3) / 1.75

            val env = if (i < attackSamples) {
                (i.toFloat() / attackSamples)
            } else {
                val progress = (i - attackSamples).toDouble() / (numSamples - attackSamples)
                Math.exp(-2.6 * progress).toFloat()
            }

            samples[i] = (waveVal * 32767.0 * volume * env).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateSilenceBuffer(durationMs: Int): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        if (numSamples <= 0) return ShortArray(0)
        return ShortArray(numSamples)
    }

    private fun playSequence(
        frequencies: List<Double>,
        durationsMs: List<Int>,
        volume: Float = 0.35f,
        type: WaveType = WaveType.SINE,
        gapMs: Int = 0
    ) {
        if (!isSoundEnabled) return

        // Compute total combined PCM buffer length
        var totalSamples = 0
        for (dur in durationsMs) {
            totalSamples += (SAMPLE_RATE * (dur / 1000.0)).toInt()
            if (gapMs > 0) {
                totalSamples += (SAMPLE_RATE * (gapMs / 1000.0)).toInt()
            }
        }
        if (totalSamples <= 0) return

        val combinedBuffer = ShortArray(totalSamples)
        var writePos = 0

        for (i in frequencies.indices) {
            val freq = frequencies[i]
            val dur = durationsMs[i]
            val toneBuffer = generateToneBuffer(freq, dur, volume, type)
            System.arraycopy(toneBuffer, 0, combinedBuffer, writePos, toneBuffer.size)
            writePos += toneBuffer.size

            if (gapMs > 0) {
                val silenceBuffer = generateSilenceBuffer(gapMs)
                System.arraycopy(silenceBuffer, 0, combinedBuffer, writePos, silenceBuffer.size)
                writePos += silenceBuffer.size
            }
        }

        // Execute fast PCM write to persistent AudioTrack on single background thread
        audioScope.launch(sfxDispatcher) {
            try {
                val track = getSfxTrack() ?: return@launch
                track.write(combinedBuffer, 0, combinedBuffer.size)
            } catch (e: Exception) {
                Log.e(TAG, "Error playing SFX sequence", e)
            }
        }
    }

    fun playTokenMove() {
        // Crisp, clear wooden token pick-up sound
        playSequence(listOf(800.0, 1150.0, 1450.0), listOf(18, 18, 22), volume = 0.40f, type = WaveType.SINE)
    }

    fun playTokenHop() {
        // High-clarity wooden token step clack sound on board ("Tuk" sound - crystal clear)
        playSequence(listOf(1250.0, 1600.0), listOf(16, 20), volume = 0.48f, type = WaveType.SINE)
    }

    fun playTurnPass() {
        // Light double-blip
        playSequence(listOf(523.3, 659.3), listOf(45, 65), volume = 0.28f, type = WaveType.SINE, gapMs = 15)
    }

    fun playAlert() {
        // Alarm-like dual sound
        playSequence(listOf(880.0, 880.0, 880.0), listOf(75, 75, 75), volume = 0.32f, type = WaveType.SINE, gapMs = 30)
    }

    fun playDiceRoll() {
        // Soft, realistic wooden dice tumbling & rolling sound ("Goti/Pasa ki smooth awaj")
        playSequence(
            listOf(240.0, 400.0, 280.0, 440.0, 330.0, 250.0, 370.0, 220.0, 190.0),
            listOf(18, 20, 18, 20, 22, 22, 25, 30, 50),
            volume = 0.38f,
            type = WaveType.SINE,
            gapMs = 8
        )
    }

    fun playTokenCaptured() {
        // High impact capture/cut sound
        val freqs = mutableListOf<Double>()
        val durs = mutableListOf<Int>()
        var f = 950.0
        while (f >= 220.0) {
            freqs.add(f)
            durs.add(14)
            f -= 85.0
        }
        playSequence(freqs, durs, volume = 0.35f, type = WaveType.TRIANGLE)
    }

    fun playVictory() {
        // Fanfare chord
        playSequence(
            listOf(261.6, 329.6, 392.0, 523.3, 659.3, 784.0, 1046.5),
            listOf(60, 60, 60, 60, 60, 60, 350),
            volume = 0.25f,
            type = WaveType.SINE,
            gapMs = 10
        )
    }

    private data class Note(val frequency: Double, val durationMs: Int)
}

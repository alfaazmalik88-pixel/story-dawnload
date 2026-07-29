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

    // Pre-computed instant SFX buffers (zero calculation delay, zero latency)
    private val tokenHopPcm: ShortArray by lazy { generateTokenHopPcm() }
    private val tokenMovePcm: ShortArray by lazy {
        generateSequenceBuffer(listOf(987.77, 1318.51), listOf(25, 35), volume = 0.40f, type = WaveType.SINE, gapMs = 5)
    }
    private val diceRollPcm: ShortArray by lazy {
        generateSequenceBuffer(listOf(360.0, 520.0, 410.0, 580.0, 320.0), listOf(14, 16, 18, 20, 28), volume = 0.42f, type = WaveType.SINE, gapMs = 8)
    }
    private val turnPassPcm: ShortArray by lazy {
        generateSequenceBuffer(listOf(523.25, 659.25), listOf(40, 50), volume = 0.30f, type = WaveType.SINE, gapMs = 10)
    }
    private val alertPcm: ShortArray by lazy {
        generateSequenceBuffer(listOf(880.0, 880.0, 880.0), listOf(60, 60, 60), volume = 0.32f, type = WaveType.SINE, gapMs = 25)
    }
    private val tokenCapturedPcm: ShortArray by lazy {
        generateSequenceBuffer(listOf(880.0, 740.0, 587.33, 440.0, 293.66), listOf(20, 20, 20, 25, 40), volume = 0.40f, type = WaveType.SINE, gapMs = 5)
    }
    private val victoryPcm: ShortArray by lazy {
        generateSequenceBuffer(listOf(261.63, 329.63, 392.00, 523.25, 659.25, 783.99, 1046.50), listOf(55, 55, 55, 55, 55, 55, 320), volume = 0.32f, type = WaveType.SINE, gapMs = 8)
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        // Warm up SFX track and preload PCM buffers at start
        audioScope.launch(sfxDispatcher) {
            getSfxTrack()
            tokenHopPcm
            tokenMovePcm
            diceRollPcm
        }
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
                // Minimum buffer size for lowest hardware latency (~20ms)
                val minBuff = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(2048)

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
                        val isGulf = currentMusicMode == "GULF"
                        val melody = if (isGulf) gulfMelody else classicMelody
                        val note = melody[noteIndex % melody.size]

                        val notePcm = if (isGulf) {
                            generateOudToneBuffer(note.frequency, note.durationMs, volume = 0.28f)
                        } else {
                            generateToneBuffer(note.frequency, note.durationMs, volume = 0.18f, type = WaveType.TRIANGLE)
                        }

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

    /**
     * Ultra-smooth tone buffer generator with Hanning/raised-cosine windowing.
     * Prevents any clicking, popping, or harmonic distortion.
     */
    private fun generateToneBuffer(
        frequency: Double,
        durationMs: Int,
        volume: Float,
        type: WaveType
    ): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        if (numSamples <= 0) return ShortArray(0)
        val samples = ShortArray(numSamples)

        val attackSamples = (SAMPLE_RATE * 0.005).toInt().coerceIn(1, numSamples)
        val releaseSamples = (SAMPLE_RATE * 0.010).toInt().coerceIn(1, numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val angle = 2.0 * Math.PI * frequency * t
            val waveVal = when (type) {
                WaveType.SINE -> Math.sin(angle)
                WaveType.SQUARE -> if (Math.sin(angle) >= 0) 0.8 else -0.8
                WaveType.TRIANGLE -> {
                    val x = angle / (2.0 * Math.PI)
                    2.0 * Math.abs(2.0 * (x - Math.floor(x + 0.5))) - 1.0
                }
            }

            val env = when {
                i < attackSamples -> 0.5 * (1.0 - Math.cos(Math.PI * i / attackSamples))
                i >= numSamples - releaseSamples -> 0.5 * (1.0 + Math.cos(Math.PI * (i - (numSamples - releaseSamples)) / releaseSamples))
                else -> 1.0
            }

            val sampleVal = (waveVal * 32767.0 * volume * env).toInt()
            samples[i] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
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
                0.5 * (1.0 - Math.cos(Math.PI * i / attackSamples))
            } else {
                val progress = (i - attackSamples).toDouble() / (numSamples - attackSamples)
                Math.exp(-2.6 * progress)
            }

            val sampleVal = (waveVal * 32767.0 * volume * env).toInt()
            samples[i] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    private fun generateSilenceBuffer(durationMs: Int): ShortArray {
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        if (numSamples <= 0) return ShortArray(0)
        return ShortArray(numSamples)
    }

    /**
     * Generates a physical wooden token landing sound ("Tuk" / "Clack") with zero phase noise / distortion.
     * Uses phase accumulation to eliminate any harmonic frequency jumps or buzzing ("banbanat").
     */
    private fun generateTokenHopPcm(): ShortArray {
        val durationMs = 28
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        val startFreq = 1350.0
        val endFreq = 480.0
        val volume = 0.50f
        val attackSamples = (SAMPLE_RATE * 0.002).toInt().coerceAtLeast(1)

        var phase = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val currentFreq = startFreq * Math.pow(endFreq / startFreq, progress)
            phase += 2.0 * Math.PI * currentFreq / SAMPLE_RATE

            // Pure sine fundamental + soft body resonance (zero phase jump noise)
            val fundamental = Math.sin(phase)
            val bodyResonance = 0.20 * Math.sin(phase * 2.0)
            val rawWave = (fundamental + bodyResonance) / 1.20

            val attack = if (i < attackSamples) (i.toDouble() / attackSamples) else 1.0
            val env = attack * Math.exp(-7.0 * progress)

            val sampleVal = (rawWave * 32767.0 * volume * env).toInt()
            buffer[i] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateSequenceBuffer(
        frequencies: List<Double>,
        durationsMs: List<Int>,
        volume: Float = 0.35f,
        type: WaveType = WaveType.SINE,
        gapMs: Int = 0
    ): ShortArray {
        var totalSamples = 0
        for (dur in durationsMs) {
            totalSamples += (SAMPLE_RATE * (dur / 1000.0)).toInt()
            if (gapMs > 0) {
                totalSamples += (SAMPLE_RATE * (gapMs / 1000.0)).toInt()
            }
        }
        if (totalSamples <= 0) return ShortArray(0)

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
        return combinedBuffer
    }

    private fun playPrecalculatedPcm(buffer: ShortArray) {
        if (!isSoundEnabled || buffer.isEmpty()) return
        audioScope.launch(sfxDispatcher) {
            try {
                val track = getSfxTrack() ?: return@launch
                track.write(buffer, 0, buffer.size, AudioTrack.WRITE_NON_BLOCKING)
            } catch (e: Exception) {
                Log.e(TAG, "Error playing precalculated SFX", e)
            }
        }
    }

    /**
     * Crystal clear, organic wooden token pick-up sound ("Tink"). Instant playback.
     */
    fun playTokenMove() {
        playPrecalculatedPcm(tokenMovePcm)
    }

    /**
     * Instant, zero-delay physical wooden token board hop sound ("Tuk" / "Clack").
     * Crystal clear, pure acoustic wood clack with 0ms calculation delay and zero buzzing ("banbanat").
     */
    fun playTokenHop() {
        playPrecalculatedPcm(tokenHopPcm)
    }

    fun playTurnPass() {
        playPrecalculatedPcm(turnPassPcm)
    }

    fun playAlert() {
        playPrecalculatedPcm(alertPcm)
    }

    /**
     * Smooth, realistic wooden dice rolling & tumbling sound ("Pasa ki crystal clear awaj").
     */
    fun playDiceRoll() {
        playPrecalculatedPcm(diceRollPcm)
    }

    fun playTokenCaptured() {
        playPrecalculatedPcm(tokenCapturedPcm)
    }

    fun playVictory() {
        playPrecalculatedPcm(victoryPcm)
    }

    private data class Note(val frequency: Double, val durationMs: Int)
}

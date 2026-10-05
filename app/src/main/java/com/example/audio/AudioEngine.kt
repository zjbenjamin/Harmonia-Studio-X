package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*
import kotlin.random.Random

class AudioEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        private const val MAX_VOICES = 32
        private const val BUFFER_SIZE_FRAMES = 1024
    }

    private var audioTrack: AudioTrack? = null
    private var isEngineRunning = false
    private var renderScope: CoroutineScope? = null

    // Real-time voices
    private val activeVoices = mutableListOf<SynthVoice>()
    private val voiceLock = Any()

    // Metronome
    @Volatile var isMetronomeEnabled: Boolean = false
    @Volatile var metronomeBeatTick: Int = -1 // 0 for beat 1, 1, 2, 3...
    private var metronomeSamplesLeft = 0
    private var metronomeFreq = 1600.0
    private var metronomePhase = 0.0

    // Master settings
    @Volatile var masterVolume: Float = 0.85f

    // Real-time Pitch Bend (-2 to +2 semitones) and Modulation (0 to 1)
    @Volatile var pitchBendSemitones: Float = 0f
    @Volatile var modulationDepth: Float = 0f

    // Live custom patch overrides
    private val patchMap = mutableMapOf<InstrumentType, InstrumentPatch>()

    init {
        for (type in InstrumentType.entries) {
            patchMap[type] = InstrumentPatch.defaultFor(type)
        }
    }

    fun getPatch(type: InstrumentType): InstrumentPatch {
        return patchMap[type] ?: InstrumentPatch.defaultFor(type)
    }

    fun updatePatch(patch: InstrumentPatch) {
        patchMap[patch.instrumentType] = patch
    }

    fun start() {
        if (isEngineRunning && audioTrack != null && audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
            if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                try {
                    audioTrack?.play()
                } catch (_: Exception) {}
            }
            return
        }
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = if (minBufSize > 0) max(minBufSize * 2, BUFFER_SIZE_FRAMES * 4) else 8192

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

            if (track.state == AudioTrack.STATE_INITIALIZED) {
                audioTrack = track
                track.play()
                isEngineRunning = true

                renderScope?.cancel()
                renderScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
                renderScope?.launch {
                    runAudioRenderLoop()
                }
            } else {
                Log.e("AudioEngine", "AudioTrack failed to initialize state=${track.state}")
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "Failed to initialize AudioTrack", e)
        }
    }

    fun stop() {
        isEngineRunning = false
        renderScope?.cancel()
        renderScope = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e("AudioEngine", "Error stopping AudioTrack", e)
        }
        audioTrack = null
        synchronized(voiceLock) {
            activeVoices.clear()
        }
    }

    fun noteOn(
        midiNote: Int,
        velocity: Float = 0.9f,
        instrument: InstrumentType = InstrumentType.GRAND_PIANO,
        trackId: Long = 0L
    ) {
        if (!isEngineRunning || audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
            start()
        }
        val patch = getPatch(instrument)
        val transposedNote = midiNote + (patch.octaveTranspose * 12)
        val clampedNote = transposedNote.coerceIn(12, 127)
        val frequency = 440.0 * 2.0.pow((clampedNote - 69).toDouble() / 12.0)

        synchronized(voiceLock) {
            // If already playing this note on this track, retrigger or release
            activeVoices.removeAll { it.midiNote == midiNote && it.trackId == trackId && !it.isReleased }
            if (activeVoices.size >= MAX_VOICES) {
                // Remove oldest voice
                activeVoices.removeAt(0)
            }
            activeVoices.add(
                SynthVoice(
                    midiNote = midiNote,
                    frequency = frequency,
                    velocity = velocity.coerceIn(0.1f, 1.0f),
                    instrument = instrument,
                    patch = patch,
                    trackId = trackId
                )
            )
        }
    }

    fun noteOff(midiNote: Int, trackId: Long = 0L) {
        synchronized(voiceLock) {
            for (voice in activeVoices) {
                if (voice.midiNote == midiNote && (trackId == 0L || voice.trackId == trackId)) {
                    voice.release()
                }
            }
        }
    }

    fun playDrum(drumNote: Int, velocity: Float = 0.95f) {
        noteOn(
            midiNote = drumNote,
            velocity = velocity,
            instrument = InstrumentType.DRUM_KIT,
            trackId = -1L
        )
    }

    fun triggerMetronome(isDownbeat: Boolean) {
        metronomeFreq = if (isDownbeat) 1800.0 else 1200.0
        metronomeSamplesLeft = (SAMPLE_RATE * 0.035).toInt() // 35ms sharp click
        metronomePhase = 0.0
    }

    private fun runAudioRenderLoop() {
        val audioBuffer = ShortArray(BUFFER_SIZE_FRAMES)
        val floatMixBuffer = FloatArray(BUFFER_SIZE_FRAMES)

        while (isEngineRunning && audioTrack != null) {
            floatMixBuffer.fill(0f)

            val currentBend = pitchBendSemitones
            val currentMod = modulationDepth

            // Render active voices
            synchronized(voiceLock) {
                val iterator = activeVoices.iterator()
                while (iterator.hasNext()) {
                    val voice = iterator.next()
                    val voiceFinished = voice.renderNextBlock(floatMixBuffer, BUFFER_SIZE_FRAMES, currentBend, currentMod)
                    if (voiceFinished) {
                        iterator.remove()
                    }
                }
            }

            // Render metronome if triggered
            if (metronomeSamplesLeft > 0) {
                val count = min(metronomeSamplesLeft, BUFFER_SIZE_FRAMES)
                val phaseIncrement = (2.0 * PI * metronomeFreq) / SAMPLE_RATE
                for (i in 0 until count) {
                    val decay = metronomeSamplesLeft.toFloat() / (SAMPLE_RATE * 0.035f)
                    val sample = (sin(metronomePhase) * decay * 0.45f).toFloat()
                    floatMixBuffer[i] += sample
                    metronomePhase += phaseIncrement
                    metronomeSamplesLeft--
                }
            }

            // Apply master gain and soft clipping (tanh limiter)
            val gain = masterVolume
            for (i in 0 until BUFFER_SIZE_FRAMES) {
                var s = floatMixBuffer[i] * gain
                // Soft limiter using tanh
                s = tanh(s)
                audioBuffer[i] = (s * 32767f).coerceIn(-32767f, 32767f).toInt().toShort()
            }

            audioTrack?.write(audioBuffer, 0, BUFFER_SIZE_FRAMES)
        }
    }

    // Voice synthesis engine
    private class SynthVoice(
        val midiNote: Int,
        val frequency: Double,
        val velocity: Float,
        val instrument: InstrumentType,
        val patch: InstrumentPatch,
        val trackId: Long
    ) {
        var phase: Double = 0.0
        var phase2: Double = 0.0
        var sampleIndex: Long = 0
        var isReleased: Boolean = false
        var releaseSampleStart: Long = 0
        var lastFilterSample = 0.0

        // Pluck simulation buffer for Karplus-Strong
        private val karplusBuffer: FloatArray? = if (
            instrument == InstrumentType.ACOUSTIC_GUITAR ||
            instrument == InstrumentType.CELTIC_HARP ||
            instrument == InstrumentType.CHINESE_GUZHENG ||
            instrument == InstrumentType.CHINESE_PIPA ||
            instrument == InstrumentType.CHINESE_YANGQIN ||
            instrument == InstrumentType.CHINESE_GUQIN
        ) {
            val period = (SAMPLE_RATE / frequency).toInt().coerceIn(10, 2048)
            FloatArray(period) { (Random.nextFloat() * 2f - 1f) }
        } else null
        private var karplusIndex = 0

        fun release() {
            if (!isReleased) {
                isReleased = true
                releaseSampleStart = sampleIndex
            }
        }

        fun renderNextBlock(buffer: FloatArray, frames: Int, pitchBend: Float = 0f, modulation: Float = 0f): Boolean {
            val attackSamples = (patch.attackMs * SAMPLE_RATE / 1000f).coerceAtLeast(1f)
            val decaySamples = (patch.decayMs * SAMPLE_RATE / 1000f).coerceAtLeast(1f)
            val releaseSamples = (patch.releaseMs * SAMPLE_RATE / 1000f).coerceAtLeast(1f)
            val sustain = patch.sustainLevel

            val bentFreq = if (pitchBend != 0f) frequency * 2.0.pow(pitchBend.toDouble() / 12.0) else frequency
            val vibrato = if (modulation > 0f) (sin(sampleIndex * 0.0007) * modulation.toDouble() * 0.025) else 0.0
            val phaseInc = (2.0 * PI * bentFreq * (1.0 + vibrato)) / SAMPLE_RATE
            var finished = false

            for (i in 0 until frames) {
                sampleIndex++

                // Envelope calculation
                val envelope: Float = if (!isReleased) {
                    if (sampleIndex < attackSamples) {
                        (sampleIndex / attackSamples)
                    } else if (sampleIndex < attackSamples + decaySamples) {
                        val progress = (sampleIndex - attackSamples) / decaySamples
                        1.0f - progress * (1.0f - sustain)
                    } else {
                        sustain
                    }
                } else {
                    val relProgress = (sampleIndex - releaseSampleStart) / releaseSamples
                    if (relProgress >= 1.0f) {
                        finished = true
                        break
                    }
                    sustain * (1.0f - relProgress)
                }

                if (envelope <= 0.001f && isReleased) {
                    finished = true
                    break
                }

                // Waveform generation based on instrument
                val rawSample = generateSample(frequency, phaseInc, sampleIndex)

                // Simple 1-pole low pass filter simulation
                val cutoffRatio = (patch.filterCutoff / (SAMPLE_RATE / 2.0)).coerceIn(0.02, 0.98)
                lastFilterSample += cutoffRatio * (rawSample - lastFilterSample)

                // Drive/Saturation
                var sample = lastFilterSample.toFloat() * envelope * velocity * patch.volume
                if (patch.drive > 0.01f) {
                    sample = (1f + patch.drive) * sample / (1f + patch.drive * abs(sample))
                }

                buffer[i] += sample
                phase = (phase + phaseInc) % (2.0 * PI)
                phase2 = (phase2 + phaseInc * 1.003) % (2.0 * PI) // subtle chorus detune
            }

            return finished
        }

        private fun generateSample(freq: Double, phaseInc: Double, currentSample: Long): Double {
            return when (instrument) {
                InstrumentType.GRAND_PIANO -> {
                    // Multi-harmonic piano model with faster decay on higher harmonics
                    val t = currentSample.toDouble() / SAMPLE_RATE
                    val h1 = sin(phase) * exp(-1.2 * t)
                    val h2 = sin(phase * 2.0) * 0.55 * exp(-2.8 * t)
                    val h3 = sin(phase * 3.0) * 0.28 * exp(-4.5 * t)
                    val h4 = sin(phase * 4.0) * 0.15 * exp(-7.0 * t)
                    // Hammer transient
                    val hammer = if (currentSample < 200) (Random.nextDouble() * 0.2) else 0.0
                    (h1 + h2 + h3 + h4 + hammer) * 0.65
                }
                InstrumentType.ELECTRIC_PIANO -> {
                    // 2-Operator FM: Sine carrier with 2x modulator
                    val modIndex = 1.8 * exp(-2.0 * (currentSample.toDouble() / SAMPLE_RATE))
                    val mod = sin(phase * 2.0) * modIndex
                    sin(phase + mod) * 0.7
                }
                InstrumentType.CHURCH_ORGAN -> {
                    // Pipe drawbar additive synthesis with vibrato
                    val vibrato = sin(2.0 * PI * 5.5 * (currentSample.toDouble() / SAMPLE_RATE)) * 0.02
                    val p = phase * (1.0 + vibrato)
                    val d16 = sin(p * 0.5) * 0.35
                    val d8 = sin(p * 1.0) * 0.50
                    val d4 = sin(p * 2.0) * 0.30
                    val d2 = sin(p * 4.0) * 0.15
                    (d16 + d8 + d4 + d2) * 0.6
                }
                InstrumentType.SYNTH_POLY_KEYS -> {
                    // Dual oscillator saw + pulse
                    val saw1 = 2.0 * (phase / (2.0 * PI)) - 1.0
                    val saw2 = 2.0 * (phase2 / (2.0 * PI)) - 1.0
                    (saw1 * 0.5 + saw2 * 0.5) * 0.6
                }
                InstrumentType.ACOUSTIC_GUITAR, InstrumentType.CELTIC_HARP -> {
                    // Karplus-Strong pluck
                    if (karplusBuffer != null) {
                        val currentVal = karplusBuffer[karplusIndex]
                        val nextIdx = (karplusIndex + 1) % karplusBuffer.size
                        val nextVal = karplusBuffer[nextIdx]
                        val filtered = (currentVal + nextVal) * 0.496f
                        karplusBuffer[karplusIndex] = filtered
                        karplusIndex = nextIdx
                        currentVal.toDouble() * 0.8
                    } else {
                        sin(phase) * 0.5
                    }
                }
                InstrumentType.CHINESE_GUZHENG -> {
                    // Plucked string with Chinese ornamentation / pitch bend vibrato
                    val vibrato = sin(2.0 * PI * 4.8 * (currentSample.toDouble() / SAMPLE_RATE)) * 0.035
                    if (karplusBuffer != null) {
                        val currentVal = karplusBuffer[karplusIndex]
                        val nextIdx = (karplusIndex + 1) % karplusBuffer.size
                        val filtered = (currentVal + karplusBuffer[nextIdx]) * 0.497f
                        karplusBuffer[karplusIndex] = filtered
                        karplusIndex = nextIdx
                        (currentVal.toDouble() + sin(phase * (1.0 + vibrato)) * 0.25) * 0.7
                    } else {
                        sin(phase * (1.0 + vibrato)) * 0.7
                    }
                }
                InstrumentType.ERHU -> {
                    // Bowed string oriental timbre with pitch vibrato and slide
                    val vibrato = sin(2.0 * PI * 6.0 * (currentSample.toDouble() / SAMPLE_RATE)) * 0.04
                    val p = phase * (1.0 + vibrato)
                    val s1 = sin(p) * 0.6
                    val s2 = sin(p * 2.0) * 0.35
                    val s3 = sin(p * 3.0) * 0.2
                    (s1 + s2 + s3) * 0.65
                }
                InstrumentType.CHINESE_PIPA -> {
                    // Four-string plucked lute with rapid tremolo roll and bright bite
                    val t = currentSample.toDouble() / SAMPLE_RATE
                    val tremolo = sin(2.0 * PI * 13.0 * t) * 0.15
                    if (karplusBuffer != null) {
                        val currentVal = karplusBuffer[karplusIndex]
                        val nextIdx = (karplusIndex + 1) % karplusBuffer.size
                        val filtered = (currentVal + karplusBuffer[nextIdx]) * 0.493f
                        karplusBuffer[karplusIndex] = filtered
                        karplusIndex = nextIdx
                        (currentVal.toDouble() * (1.0 + tremolo) + sin(phase * 2.0) * 0.2) * 0.8
                    } else sin(phase) * 0.7
                }
                InstrumentType.CHINESE_DIZI -> {
                    // Bamboo flute with buzzing dimo (笛膜) resonance and breath noise
                    val t = currentSample.toDouble() / SAMPLE_RATE
                    val vibrato = sin(2.0 * PI * 5.8 * t) * 0.02
                    val dimoBuzz = sin(phase * 3.0) * 0.25 * sin(phase * 5.0)
                    val breath = (Random.nextDouble() * 2.0 - 1.0) * 0.04
                    (sin(phase * (1.0 + vibrato)) * 0.65 + dimoBuzz + breath) * 0.75
                }
                InstrumentType.CHINESE_YANGQIN -> {
                    // Hammered dulcimer: dual-strike attack and metallic resonance
                    val t = currentSample.toDouble() / SAMPLE_RATE
                    val strike = if (t < 0.012) sin(phase * 4.0) * 0.4 else 0.0
                    if (karplusBuffer != null) {
                        val currentVal = karplusBuffer[karplusIndex]
                        val nextIdx = (karplusIndex + 1) % karplusBuffer.size
                        val filtered = (currentVal + karplusBuffer[nextIdx]) * 0.496f
                        karplusBuffer[karplusIndex] = filtered
                        karplusIndex = nextIdx
                        (currentVal.toDouble() + strike + sin(phase * 3.0) * 0.15) * 0.75
                    } else sin(phase) * 0.7
                }
                InstrumentType.CHINESE_SUONA -> {
                    // Double-reed brass horn with piercing festive harmonics
                    val s1 = sin(phase) * 0.55
                    val s2 = sin(phase * 3.0) * 0.35
                    val s3 = sin(phase * 5.0) * 0.22
                    val s4 = sin(phase * 7.0) * 0.14
                    val squelch = if (sin(phase) > 0) 0.1 else -0.1
                    (s1 + s2 + s3 + s4 + squelch) * 0.75
                }
                InstrumentType.CHINESE_GUQIN -> {
                    // Seven-string silk zither: deep resonant fundamental and slow decay
                    val t = currentSample.toDouble() / SAMPLE_RATE
                    val slide = sin(2.0 * PI * 2.5 * t) * 0.015
                    if (karplusBuffer != null) {
                        val currentVal = karplusBuffer[karplusIndex]
                        val nextIdx = (karplusIndex + 1) % karplusBuffer.size
                        val filtered = (currentVal + karplusBuffer[nextIdx]) * 0.498f
                        karplusBuffer[karplusIndex] = filtered
                        karplusIndex = nextIdx
                        (currentVal.toDouble() + sin(phase * (1.0 + slide) * 0.5) * 0.25) * 0.75
                    } else sin(phase) * 0.7
                }
                InstrumentType.CHINESE_BIANZHONG -> {
                    // Imperial bronze chimes: two-tone strike harmonics and long decay
                    val t = currentSample.toDouble() / SAMPLE_RATE
                    val b1 = sin(phase) * exp(-1.5 * t)
                    val b2 = sin(phase * 1.54) * 0.6 * exp(-2.2 * t)
                    val b3 = sin(phase * 2.76) * 0.35 * exp(-3.8 * t)
                    val b4 = sin(phase * 4.12) * 0.18 * exp(-6.0 * t)
                    (b1 + b2 + b3 + b4) * 0.75
                }
                InstrumentType.ORCHESTRAL_STRINGS -> {
                    // Detuned lush ensemble
                    val s1 = sin(phase)
                    val s2 = sin(phase2)
                    val s3 = sin(phase * 1.006)
                    (s1 + s2 + s3) * 0.28
                }
                InstrumentType.CELLO -> {
                    val s1 = sin(phase) * 0.65
                    val s2 = sin(phase * 2.0) * 0.4
                    val s3 = sin(phase * 3.0) * 0.25
                    (s1 + s2 + s3) * 0.55
                }
                InstrumentType.BRASS_SECTION -> {
                    // Sawtooth with brass flare
                    val saw = 2.0 * (phase / (2.0 * PI)) - 1.0
                    val sawDetune = 2.0 * (phase2 / (2.0 * PI)) - 1.0
                    (saw * 0.5 + sawDetune * 0.5) * 0.65
                }
                InstrumentType.TRUMPET -> {
                    val s1 = sin(phase) * 0.5
                    val s2 = sin(phase * 2.0) * 0.35
                    val s3 = sin(phase * 3.0) * 0.3
                    val s4 = sin(phase * 4.0) * 0.15
                    (s1 + s2 + s3 + s4) * 0.6
                }
                InstrumentType.SAXOPHONE -> {
                    val sq = if (sin(phase) > 0) 0.6 else -0.6
                    val sine = sin(phase) * 0.5
                    (sq * 0.45 + sine * 0.55) * 0.65
                }
                InstrumentType.FLUTE -> {
                    val t = currentSample.toDouble() / SAMPLE_RATE
                    val flutter = sin(2.0 * PI * 5.0 * t) * 0.015
                    val breath = (Random.nextDouble() * 2.0 - 1.0) * 0.05
                    (sin(phase * (1.0 + flutter)) + breath) * 0.7
                }
                InstrumentType.SLAP_BASS -> {
                    val snap = if (currentSample < 500) (sin(phase * 4.0) * 0.7) else 0.0
                    val sub = sin(phase * 0.5) * 0.4
                    val body = sin(phase) * 0.55
                    (snap + sub + body) * 0.7
                }
                InstrumentType.SUB_808_BASS -> {
                    // Pitch drop on initial trigger
                    val t = currentSample.toDouble() / SAMPLE_RATE
                    val pitchDrop = exp(-12.0 * t) * 1.5
                    val dropFreq = freq * (1.0 + pitchDrop)
                    val pInc = (2.0 * PI * dropFreq) / SAMPLE_RATE
                    sin(phase + pInc) * 0.85
                }
                InstrumentType.ACID_303_BASS -> {
                    val saw = 2.0 * (phase / (2.0 * PI)) - 1.0
                    saw * 0.7
                }
                InstrumentType.MARIMBA -> {
                    val t = currentSample.toDouble() / SAMPLE_RATE
                    val h1 = sin(phase) * exp(-8.0 * t)
                    val h4 = sin(phase * 4.0) * 0.3 * exp(-16.0 * t)
                    (h1 + h4) * 0.8
                }
                InstrumentType.DRUM_KIT -> {
                    renderDrumSample(midiNote, currentSample)
                }
            }
        }

        private fun renderDrumSample(note: Int, sampleIdx: Long): Double {
            val t = sampleIdx.toDouble() / SAMPLE_RATE
            return when (note) {
                // Kick (36)
                36 -> {
                    if (t > 0.4) return 0.0
                    val pitch = 150.0 * exp(-32.0 * t) + 48.0
                    val pInc = (2.0 * PI * pitch) / SAMPLE_RATE
                    val s = sin(sampleIdx * pInc) * exp(-6.5 * t)
                    tanh(s * 1.5) * 0.9
                }
                // Snare (38)
                38 -> {
                    if (t > 0.35) return 0.0
                    val tone = sin(2.0 * PI * 185.0 * t) * exp(-15.0 * t) * 0.4
                    val noise = (Random.nextDouble() * 2.0 - 1.0) * exp(-12.0 * t) * 0.6
                    (tone + noise) * 0.85
                }
                // Closed Hi-Hat (42)
                42 -> {
                    if (t > 0.06) return 0.0
                    val noise = (Random.nextDouble() * 2.0 - 1.0) * exp(-55.0 * t)
                    noise * 0.6
                }
                // Open Hi-Hat (46)
                46 -> {
                    if (t > 0.4) return 0.0
                    val noise = (Random.nextDouble() * 2.0 - 1.0) * exp(-9.0 * t)
                    noise * 0.65
                }
                // Hand Clap (39)
                39 -> {
                    if (t > 0.3) return 0.0
                    // Triple staggered burst
                    val burst = if (t < 0.015 || (t in 0.025..0.04) || (t in 0.05..0.07)) {
                        Random.nextDouble() * 2.0 - 1.0
                    } else {
                        (Random.nextDouble() * 2.0 - 1.0) * exp(-14.0 * (t - 0.07).coerceAtLeast(0.0))
                    }
                    burst * 0.7
                }
                // Low Tom (41)
                41 -> {
                    if (t > 0.4) return 0.0
                    val pitch = 120.0 * exp(-10.0 * t) + 65.0
                    sin(2.0 * PI * pitch * t) * exp(-8.0 * t) * 0.8
                }
                // Mid Tom (45)
                45 -> {
                    if (t > 0.35) return 0.0
                    val pitch = 160.0 * exp(-12.0 * t) + 95.0
                    sin(2.0 * PI * pitch * t) * exp(-9.0 * t) * 0.8
                }
                // High Tom (48)
                48 -> {
                    if (t > 0.3) return 0.0
                    val pitch = 210.0 * exp(-14.0 * t) + 130.0
                    sin(2.0 * PI * pitch * t) * exp(-10.0 * t) * 0.8
                }
                // Cowbell (56)
                56 -> {
                    if (t > 0.25) return 0.0
                    val sq1 = if (sin(2.0 * PI * 587.0 * t) > 0) 1.0 else -1.0
                    val sq2 = if (sin(2.0 * PI * 845.0 * t) > 0) 1.0 else -1.0
                    (sq1 * 0.5 + sq2 * 0.5) * exp(-15.0 * t) * 0.65
                }
                // Default percussion
                else -> {
                    if (t > 0.2) return 0.0
                    (Random.nextDouble() * 2.0 - 1.0) * exp(-18.0 * t) * 0.5
                }
            }
        }
    }

    /**
     * Offline High-Fidelity Stereo WAV file exporter:
     * Renders a multi-track arrangement into a pristine 16-bit 44.1kHz Stereo WAV file.
     */
    fun exportWav(
        outputFile: File,
        totalDurationSeconds: Float,
        bpm: Int,
        normalize: Boolean = true,
        onProgress: ((Float) -> Unit)? = null,
        renderNotesProvider: () -> List<ExportNoteEvent>
    ): Boolean {
        try {
            val totalFrames = (totalDurationSeconds * SAMPLE_RATE).toInt()
            val notes = renderNotesProvider()
            val leftBuffer = FloatArray(totalFrames)
            val rightBuffer = FloatArray(totalFrames)

            val totalNotes = notes.size
            for ((idx, noteEvent) in notes.withIndex()) {
                val startFrame = ((noteEvent.startBeat * 60f / bpm) * SAMPLE_RATE).toInt()
                val durationFrames = ((noteEvent.durationBeats * 60f / bpm) * SAMPLE_RATE).toInt()
                val patch = getPatch(noteEvent.instrument)
                val voice = SynthVoice(
                    midiNote = noteEvent.midiNote,
                    frequency = 440.0 * 2.0.pow((noteEvent.midiNote - 69).toDouble() / 12.0),
                    velocity = noteEvent.velocity,
                    instrument = noteEvent.instrument,
                    patch = patch,
                    trackId = 0L
                )

                val block = FloatArray(durationFrames)
                voice.renderNextBlock(block, durationFrames)
                voice.release()
                val tail = FloatArray((patch.releaseMs * SAMPLE_RATE / 1000f).toInt())
                voice.renderNextBlock(tail, tail.size)

                val pan = noteEvent.trackPan.coerceIn(-1f, 1f)
                val leftPan = cos((pan + 1f) * PI / 4.0).toFloat()
                val rightPan = sin((pan + 1f) * PI / 4.0).toFloat()
                val vol = noteEvent.trackVolume

                for (f in 0 until block.size) {
                    val targetIdx = startFrame + f
                    if (targetIdx < totalFrames) {
                        val s = block[f] * vol
                        leftBuffer[targetIdx] += s * leftPan
                        rightBuffer[targetIdx] += s * rightPan
                    }
                }
                for (f in 0 until tail.size) {
                    val targetIdx = startFrame + durationFrames + f
                    if (targetIdx < totalFrames) {
                        val s = tail[f] * vol
                        leftBuffer[targetIdx] += s * leftPan
                        rightBuffer[targetIdx] += s * rightPan
                    }
                }

                if (totalNotes > 0 && idx % 10 == 0) {
                    onProgress?.invoke((idx.toFloat() / totalNotes) * 0.7f)
                }
            }

            // Normalization
            var maxPeak = 0.001f
            for (i in 0 until totalFrames) {
                val l = abs(leftBuffer[i])
                val r = abs(rightBuffer[i])
                if (l > maxPeak) maxPeak = l
                if (r > maxPeak) maxPeak = r
            }

            val targetGain = if (normalize && maxPeak > 0.01f) {
                (0.95f / maxPeak).coerceAtMost(2.5f)
            } else 1.0f

            // 16-bit PCM Stereo WAV Header
            val numChannels = 2
            val bytesPerSample = 2
            val subChunk2Size = totalFrames * numChannels * bytesPerSample
            val chunkSize = 36 + subChunk2Size

            val byteBuffer = ByteBuffer.allocate(44 + subChunk2Size).order(ByteOrder.LITTLE_ENDIAN)

            // RIFF chunk
            byteBuffer.put("RIFF".toByteArray())
            byteBuffer.putInt(chunkSize)
            byteBuffer.put("WAVE".toByteArray())
            // fmt sub-chunk
            byteBuffer.put("fmt ".toByteArray())
            byteBuffer.putInt(16) // Subchunk1Size
            byteBuffer.putShort(1) // PCM format
            byteBuffer.putShort(numChannels.toShort()) // 2 channels (Stereo)
            byteBuffer.putInt(SAMPLE_RATE)
            byteBuffer.putInt(SAMPLE_RATE * numChannels * bytesPerSample) // ByteRate
            byteBuffer.putShort((numChannels * bytesPerSample).toShort()) // BlockAlign (4)
            byteBuffer.putShort(16) // BitsPerSample
            // data sub-chunk
            byteBuffer.put("data".toByteArray())
            byteBuffer.putInt(subChunk2Size)

            for (i in 0 until totalFrames) {
                var l = tanh(leftBuffer[i] * targetGain)
                var r = tanh(rightBuffer[i] * targetGain)

                val sL = (l * 32767f).coerceIn(-32767f, 32767f).toInt().toShort()
                val sR = (r * 32767f).coerceIn(-32767f, 32767f).toInt().toShort()

                byteBuffer.putShort(sL)
                byteBuffer.putShort(sR)

                if (i % 20000 == 0) {
                    onProgress?.invoke(0.7f + (i.toFloat() / totalFrames) * 0.3f)
                }
            }

            onProgress?.invoke(1.0f)

            FileOutputStream(outputFile).use { fos ->
                fos.write(byteBuffer.array())
            }
            return true
        } catch (e: Exception) {
            Log.e("AudioEngine", "Failed to export Stereo WAV", e)
            return false
        }
    }

    data class ExportNoteEvent(
        val midiNote: Int,
        val startBeat: Float,
        val durationBeats: Float,
        val velocity: Float,
        val instrument: InstrumentType,
        val trackVolume: Float,
        val trackPan: Float = 0f
    )
}

package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.example.midi.MidiNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

data class DecodedAudioData(
    val samples: FloatArray,
    val sampleRate: Int,
    val channelCount: Int,
    val durationSeconds: Float
)

data class AudioAnalysisResult(
    val notes: List<MidiNote>,
    val detectedKey: String,
    val sampleCount: Int,
    val durationSeconds: Float,
    val originalFileName: String
)

object AudioDecoder {
    private const val TAG = "AudioDecoder"

    /**
     * Decodes ANY audio file format (MP3, WAV, AAC, M4A, FLAC, OGG, Opus)
     * using Android's native MediaExtractor + MediaCodec to 32-bit float PCM samples.
     */
    suspend fun decodeAudioToFloatPcm(
        context: Context,
        uri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): DecodedAudioData = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = trackFormat
                    break
                }
            }

            if (audioTrackIndex < 0 || format == null) {
                throw IllegalArgumentException("No audio track found in selected media file")
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: "audio/raw"
            val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
            val durationSec = durationUs / 1_000_000f

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val pcmBytesOut = ByteArrayOutputStream()
            val info = MediaCodec.BufferInfo()
            var isInputEof = false
            var isOutputEof = false
            val timeoutUs = 8000L

            while (!isOutputEof) {
                if (!isInputEof) {
                    val inputBufIndex = codec.dequeueInputBuffer(timeoutUs)
                    if (inputBufIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isInputEof = true
                            } else {
                                val presentationTimeUs = extractor.sampleTime
                                codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, 0)
                                extractor.advance()
                                if (durationUs > 0) {
                                    val progress = (presentationTimeUs.toFloat() / durationUs).coerceIn(0f, 0.5f)
                                    onProgress?.invoke(progress)
                                }
                            }
                        }
                    }
                }

                val outputBufIndex = codec.dequeueOutputBuffer(info, timeoutUs)
                if (outputBufIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufIndex)
                    if (outputBuffer != null && info.size > 0) {
                        outputBuffer.position(info.offset)
                        outputBuffer.limit(info.offset + info.size)
                        val chunk = ByteArray(info.size)
                        outputBuffer.get(chunk)
                        pcmBytesOut.write(chunk)
                    }
                    codec.releaseOutputBuffer(outputBufIndex, false)

                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isOutputEof = true
                    }
                }
            }

            val rawBytes = pcmBytesOut.toByteArray()
            val shortCount = rawBytes.size / 2
            val shortBuffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val floatMonoSamples: FloatArray

            if (channelCount > 1) {
                // Downmix stereo / multi-channel to mono
                val frameCount = shortCount / channelCount
                floatMonoSamples = FloatArray(frameCount)
                for (f in 0 until frameCount) {
                    var sum = 0f
                    for (ch in 0 until channelCount) {
                        val s = shortBuffer.get(f * channelCount + ch) / 32768.0f
                        sum += s
                    }
                    floatMonoSamples[f] = sum / channelCount
                }
            } else {
                floatMonoSamples = FloatArray(shortCount)
                for (i in 0 until shortCount) {
                    floatMonoSamples[i] = shortBuffer.get(i) / 32768.0f
                }
            }

            onProgress?.invoke(0.6f)
            DecodedAudioData(
                samples = floatMonoSamples,
                sampleRate = sampleRate,
                channelCount = channelCount,
                durationSeconds = if (durationSec > 0f) durationSec else (floatMonoSamples.size.toFloat() / sampleRate)
            )
        } finally {
            try {
                codec?.stop()
                codec?.release()
                extractor.release()
            } catch (e: Exception) {
                Log.w(TAG, "Cleanup exception", e)
            }
        }
    }
}

object AudioToMidiAnalyzer {
    private const val TAG = "AudioToMidiAnalyzer"

    /**
     * Analyzes float PCM audio samples and converts them into MIDI notes
     * using Onset Detection + Time-domain YIN fundamental pitch tracking ($f_0$).
     */
    suspend fun analyzeAudioToMidi(
        audioData: DecodedAudioData,
        bpm: Int,
        originalFileName: String,
        sensitivity: Float = 0.65f, // 0.1 to 1.0
        minNoteDurationSec: Float = 0.08f,
        onProgress: ((Float) -> Unit)? = null
    ): AudioAnalysisResult = withContext(Dispatchers.Default) {
        val samples = audioData.samples
        val sampleRate = audioData.sampleRate
        val totalSamples = samples.size

        if (totalSamples == 0) {
            return@withContext AudioAnalysisResult(emptyList(), "C Major", 0, 0f, originalFileName)
        }

        // Frame parameters
        val hopSize = 512 // ~11.6ms hop at 44.1kHz
        val windowSize = 2048 // ~46.4ms analysis window
        val frameCount = (totalSamples - windowSize) / hopSize

        val energies = FloatArray(max(0, frameCount))
        val pitches = IntArray(max(0, frameCount)) { -1 }

        // Step 1: Compute RMS energy & Pitch for each frame
        for (f in 0 until frameCount) {
            val startIdx = f * hopSize
            var sumSquare = 0f
            for (i in 0 until windowSize) {
                val s = samples[startIdx + i]
                sumSquare += s * s
            }
            val rms = sqrt(sumSquare / windowSize)
            energies[f] = rms

            // Pitch detection if energy is above silence threshold
            val threshold = (0.012f * (1.2f - sensitivity)).coerceAtLeast(0.003f)
            if (rms >= threshold) {
                val pitch = detectPitchYin(samples, startIdx, windowSize, sampleRate)
                pitches[f] = pitch
            }

            if (f % 50 == 0 && frameCount > 0) {
                onProgress?.invoke(0.6f + (f.toFloat() / frameCount) * 0.3f)
            }
        }

        // Step 2: Segment contiguous pitch frames into discrete MidiNotes
        val detectedNotes = mutableListOf<MidiNote>()
        val secondsPerBeat = 60.0f / bpm

        var currentPitch = -1
        var noteStartFrame = 0
        var maxEnergyInNote = 0f

        for (f in 0 until frameCount) {
            val p = pitches[f]
            val e = energies[f]

            if (p != currentPitch) {
                // End previous note if valid
                if (currentPitch in 21..108) {
                    val noteDurationSec = (f - noteStartFrame) * (hopSize.toFloat() / sampleRate)
                    if (noteDurationSec >= minNoteDurationSec) {
                        val startSec = noteStartFrame * (hopSize.toFloat() / sampleRate)
                        val startBeat = startSec / secondsPerBeat
                        val durationBeats = max(0.25f, noteDurationSec / secondsPerBeat)
                        val velocity = (maxEnergyInNote * 3.5f).coerceIn(0.4f, 1.0f)

                        detectedNotes.add(
                            MidiNote(
                                pitch = currentPitch,
                                startBeat = (round(startBeat * 4f) / 4f), // 16th-note quantize
                                durationBeats = (round(durationBeats * 4f) / 4f).coerceAtLeast(0.25f),
                                velocity = velocity
                            )
                        )
                    }
                }
                currentPitch = p
                noteStartFrame = f
                maxEnergyInNote = e
            } else {
                if (e > maxEnergyInNote) maxEnergyInNote = e
            }
        }

        // Final note
        if (currentPitch in 21..108) {
            val noteDurationSec = (frameCount - noteStartFrame) * (hopSize.toFloat() / sampleRate)
            if (noteDurationSec >= minNoteDurationSec) {
                val startSec = noteStartFrame * (hopSize.toFloat() / sampleRate)
                val startBeat = startSec / secondsPerBeat
                val durationBeats = max(0.25f, noteDurationSec / secondsPerBeat)
                detectedNotes.add(
                    MidiNote(
                        pitch = currentPitch,
                        startBeat = (round(startBeat * 4f) / 4f),
                        durationBeats = (round(durationBeats * 4f) / 4f).coerceAtLeast(0.25f),
                        velocity = 0.85f
                    )
                )
            }
        }

        // Estimate key from detected pitch classes
        val estimatedKey = estimateMusicalKey(detectedNotes)
        onProgress?.invoke(1.0f)

        AudioAnalysisResult(
            notes = detectedNotes,
            detectedKey = estimatedKey,
            sampleCount = totalSamples,
            durationSeconds = audioData.durationSeconds,
            originalFileName = originalFileName
        )
    }

    /**
     * YIN Fundamental Frequency ($f_0$) pitch detection algorithm.
     * Computes cumulative mean normalized difference function to find period tau.
     */
    private fun detectPitchYin(
        samples: FloatArray,
        offset: Int,
        windowSize: Int,
        sampleRate: Int
    ): Int {
        val halfWindow = windowSize / 2
        val minPeriod = (sampleRate / 1000.0).toInt().coerceAtLeast(10) // up to 1000 Hz (~C6)
        val maxPeriod = (sampleRate / 45.0).toInt().coerceAtMost(halfWindow - 1) // down to 45 Hz (~F#1)

        val diff = DoubleArray(halfWindow)

        // 1. Difference function
        for (tau in minPeriod..maxPeriod) {
            var sum = 0.0
            for (i in 0 until halfWindow) {
                val delta = samples[offset + i] - samples[offset + i + tau]
                sum += delta * delta
            }
            diff[tau] = sum
        }

        // 2. Cumulative mean normalized difference function
        var runningSum = 0.0
        val cmnd = DoubleArray(halfWindow)
        cmnd[0] = 1.0

        for (tau in 1 until halfWindow) {
            runningSum += diff[tau]
            cmnd[tau] = if (runningSum > 0.0) diff[tau] / (runningSum / tau) else 1.0
        }

        // 3. Absolute thresholding for lowest period
        val threshold = 0.15
        var bestTau = -1
        for (tau in minPeriod..maxPeriod) {
            if (cmnd[tau] < threshold) {
                // Find local minimum
                var localMin = tau
                while (localMin + 1 < halfWindow && cmnd[localMin + 1] < cmnd[localMin]) {
                    localMin++
                }
                bestTau = localMin
                break
            }
        }

        // If no minimum below threshold, find global minimum in range
        if (bestTau == -1) {
            var minVal = 100.0
            for (tau in minPeriod..maxPeriod) {
                if (cmnd[tau] < minVal) {
                    minVal = cmnd[tau]
                    bestTau = tau
                }
            }
            if (minVal > 0.45) return -1 // Not voiced / noisy
        }

        if (bestTau <= 0) return -1

        // Parabolic interpolation for fine tuning tau
        val s0 = if (bestTau > 0) cmnd[bestTau - 1] else cmnd[bestTau]
        val s1 = cmnd[bestTau]
        val s2 = if (bestTau + 1 < halfWindow) cmnd[bestTau + 1] else cmnd[bestTau]
        val fineTau = bestTau + ((s2 - s0) / (2.0 * (2.0 * s1 - s2 - s0))).coerceIn(-0.5, 0.5)

        val fundamentalFreq = sampleRate / fineTau
        if (fundamentalFreq < 30.0 || fundamentalFreq > 2000.0) return -1

        // Convert f0 to MIDI note number: 69 + 12 * log2(f0 / 440)
        val midiNumber = round(69.0 + 12.0 * (ln(fundamentalFreq / 440.0) / ln(2.0))).toInt()
        return midiNumber.coerceIn(21, 108)
    }

    private fun estimateMusicalKey(notes: List<MidiNote>): String {
        if (notes.isEmpty()) return "C Major"
        val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val pitchCounts = IntArray(12)
        for (n in notes) {
            val pc = (n.pitch % 12 + 12) % 12
            pitchCounts[pc]++
        }
        var maxPc = 0
        var maxCount = -1
        for (pc in 0 until 12) {
            if (pitchCounts[pc] > maxCount) {
                maxCount = pitchCounts[pc]
                maxPc = pc
            }
        }
        val tonic = noteNames[maxPc]
        // Check minor third (pc + 3) vs major third (pc + 4)
        val minorThirdCount = pitchCounts[(maxPc + 3) % 12]
        val majorThirdCount = pitchCounts[(maxPc + 4) % 12]
        val mode = if (minorThirdCount > majorThirdCount) "Minor" else "Major"
        return "$tonic $mode"
    }

    /**
     * Exports a list of MidiNotes into a Standard MIDI File (.mid) binary file.
     */
    fun exportMidiFile(notes: List<MidiNote>, outputFile: File, bpm: Int = 120): Boolean {
        try {
            val ticksPerQuarterNote = 480
            val sortedNotes = notes.sortedBy { it.startBeat }

            val trackEvents = mutableListOf<Pair<Long, ByteArray>>()

            for (note in sortedNotes) {
                val startTick = (note.startBeat * ticksPerQuarterNote).toLong().coerceAtLeast(0L)
                val durationTicks = (note.durationBeats * ticksPerQuarterNote).toLong().coerceAtLeast(60L)
                val endTick = startTick + durationTicks
                val vel = (note.velocity * 127).toInt().coerceIn(1, 127).toByte()
                val pitch = note.pitch.coerceIn(0, 127).toByte()

                // Note-On: Channel 0 (0x90), Pitch, Velocity
                trackEvents.add(startTick to byteArrayOf(0x90.toByte(), pitch, vel))
                // Note-Off: Channel 0 (0x80), Pitch, 0
                trackEvents.add(endTick to byteArrayOf(0x80.toByte(), pitch, 0x40.toByte()))
            }

            // Sort track events by absolute tick
            trackEvents.sortBy { it.first }

            // Convert to Delta-Time MIDI Track Stream
            val trackBytesOut = ByteArrayOutputStream()

            // Set Tempo Meta Event (500000 microseconds per quarter note for 120 BPM)
            val usPerQuarter = (60_000_000 / bpm)
            trackBytesOut.write(writeVarLen(0))
            trackBytesOut.write(byteArrayOf(0xFF.toByte(), 0x51.toByte(), 0x03.toByte()))
            trackBytesOut.write(byteArrayOf(((usPerQuarter shr 16) and 0xFF).toByte(), ((usPerQuarter shr 8) and 0xFF).toByte(), (usPerQuarter and 0xFF).toByte()))

            var lastTick = 0L
            for ((tick, event) in trackEvents) {
                val delta = tick - lastTick
                trackBytesOut.write(writeVarLen(delta))
                trackBytesOut.write(event)
                lastTick = tick
            }

            // End of Track Meta Event
            trackBytesOut.write(writeVarLen(0))
            trackBytesOut.write(byteArrayOf(0xFF.toByte(), 0x2F.toByte(), 0x00.toByte()))

            val trackBytes = trackBytesOut.toByteArray()

            // Construct Final Standard MIDI File (SMF Type 0)
            FileOutputStream(outputFile).use { fos ->
                // Header Chunk MThd
                fos.write("MThd".toByteArray())
                fos.write(intToBytes(6)) // Header length
                fos.write(shortToBytes(0)) // Format 0 (Single Track)
                fos.write(shortToBytes(1)) // Number of tracks (1)
                fos.write(shortToBytes(ticksPerQuarterNote)) // Division ticks

                // Track Chunk MTrk
                fos.write("MTrk".toByteArray())
                fos.write(intToBytes(trackBytes.size))
                fos.write(trackBytes)
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export MIDI file", e)
            return false
        }
    }

    private fun writeVarLen(value: Long): ByteArray {
        var v = value
        val buffer = ByteArray(8)
        var count = 0
        buffer[count++] = (v and 0x7F).toByte()
        v = v shr 7
        while (v > 0) {
            buffer[count++] = ((v and 0x7F) or 0x80).toByte()
            v = v shr 7
        }
        val result = ByteArray(count)
        for (i in 0 until count) {
            result[i] = buffer[count - 1 - i]
        }
        return result
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            ((value shr 24) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }

    private fun shortToBytes(value: Int): ByteArray {
        return byteArrayOf(
            ((value shr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }
}

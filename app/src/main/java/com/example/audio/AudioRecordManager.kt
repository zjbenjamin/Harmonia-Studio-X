package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

data class AudioRecordingState(
    val isRecording: Boolean = false,
    val durationSeconds: Float = 0f,
    val currentDbLevel: Float = -60f, // -60dB to 0dB
    val recordedFile: File? = null
)

class AudioRecordManager(private val context: Context) {
    private val TAG = "AudioRecordManager"
    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _recordingState = MutableStateFlow(AudioRecordingState())
    val recordingState: StateFlow<AudioRecordingState> = _recordingState.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startRecording(targetFile: File? = null): Boolean {
        if (_recordingState.value.isRecording) return true

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize <= 0) {
            Log.e(TAG, "Invalid buffer size: $minBufferSize")
            return false
        }

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize * 2
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                record.release()
                return false
            }

            val outputFile = targetFile ?: File(context.cacheDir, "rec_${System.currentTimeMillis()}.wav")
            audioRecord = record
            record.startRecording()

            _recordingState.value = AudioRecordingState(
                isRecording = true,
                durationSeconds = 0f,
                currentDbLevel = -60f,
                recordedFile = outputFile
            )

            recordingJob = coroutineScope.launch {
                val buffer = ShortArray(minBufferSize / 2)
                val fos = FileOutputStream(outputFile)
                // Write 44-byte dummy WAV header to be updated later
                fos.write(ByteArray(44))

                var totalBytesWritten = 0
                val startTime = System.currentTimeMillis()

                try {
                    while (isActive && _recordingState.value.isRecording) {
                        val readCount = record.read(buffer, 0, buffer.size)
                        if (readCount > 0) {
                            // Calculate RMS volume level
                            var sumSquare = 0.0
                            val byteBuf = ByteBuffer.allocate(readCount * 2).order(ByteOrder.LITTLE_ENDIAN)
                            for (i in 0 until readCount) {
                                val s = buffer[i]
                                byteBuf.putShort(s)
                                sumSquare += (s.toDouble() / 32768.0) * (s.toDouble() / 32768.0)
                            }
                            val rms = sqrt(sumSquare / readCount)
                            val db = (20.0 * log10(rms.coerceAtLeast(0.0001))).toFloat().coerceIn(-60f, 0f)

                            fos.write(byteBuf.array(), 0, readCount * 2)
                            totalBytesWritten += readCount * 2

                            val durationSec = (System.currentTimeMillis() - startTime) / 1000f
                            _recordingState.value = _recordingState.value.copy(
                                durationSeconds = durationSec,
                                currentDbLevel = db
                            )
                        }
                    }
                } finally {
                    fos.flush()
                    fos.close()
                    // Write true WAV header
                    updateWavHeader(outputFile, totalBytesWritten, SAMPLE_RATE, 1)
                }
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio recording", e)
            return false
        }
    }

    fun stopRecording(): File? {
        if (!_recordingState.value.isRecording) return _recordingState.value.recordedFile

        val file = _recordingState.value.recordedFile
        try {
            _recordingState.value = _recordingState.value.copy(isRecording = false)
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordingJob?.cancel()
            recordingJob = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recording", e)
        }
        return file
    }

    private fun updateWavHeader(file: File, totalAudioLen: Int, sampleRate: Int, channels: Int) {
        try {
            val totalDataLen = totalAudioLen + 36
            val byteRate = sampleRate * channels * 2

            val header = ByteArray(44)
            val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

            // RIFF chunk
            bb.put("RIFF".toByteArray())
            bb.putInt(totalDataLen)
            bb.put("WAVE".toByteArray())

            // fmt subchunk
            bb.put("fmt ".toByteArray())
            bb.putInt(16) // Subchunk1Size (16 for PCM)
            bb.putShort(1) // AudioFormat 1 = PCM
            bb.putShort(channels.toShort())
            bb.putInt(sampleRate)
            bb.putInt(byteRate)
            bb.putShort((channels * 2).toShort()) // BlockAlign
            bb.putShort(16) // BitsPerSample

            // data subchunk
            bb.put("data".toByteArray())
            bb.putInt(totalAudioLen)

            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(0)
                raf.write(header)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update WAV header", e)
        }
    }
}

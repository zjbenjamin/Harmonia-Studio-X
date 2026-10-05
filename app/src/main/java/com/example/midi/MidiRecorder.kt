package com.example.midi

import kotlin.math.round

class MidiRecorder {

    data class ActiveRecordingNote(
        val pitch: Int,
        val startBeat: Float,
        val velocity: Float
    )

    private val ongoingNotes = mutableMapOf<Int, ActiveRecordingNote>()
    private val recordedNotes = mutableListOf<MidiNote>()

    @Volatile var isRecording: Boolean = false
    var quantizeGrid: QuantizeGrid = QuantizeGrid.SIXTEENTH

    fun startRecording() {
        ongoingNotes.clear()
        recordedNotes.clear()
        isRecording = true
    }

    fun stopRecording(): List<MidiNote> {
        isRecording = false
        // Flush any unreleased notes
        val remaining = ongoingNotes.values.toList()
        for (active in remaining) {
            val note = MidiNote(
                pitch = active.pitch,
                startBeat = quantizeBeat(active.startBeat),
                durationBeats = 1.0f,
                velocity = active.velocity
            )
            recordedNotes.add(note)
        }
        ongoingNotes.clear()
        return recordedNotes.toList()
    }

    fun onNoteOn(pitch: Int, currentBeat: Float, velocity: Float) {
        if (!isRecording) return
        ongoingNotes[pitch] = ActiveRecordingNote(pitch, currentBeat, velocity)
    }

    fun onNoteOff(pitch: Int, currentBeat: Float) {
        if (!isRecording) return
        val active = ongoingNotes.remove(pitch) ?: return
        val rawDuration = (currentBeat - active.startBeat).coerceAtLeast(0.125f)

        val finalStart = quantizeBeat(active.startBeat)
        val finalDuration = quantizeDuration(rawDuration)

        recordedNotes.add(
            MidiNote(
                pitch = active.pitch,
                startBeat = finalStart,
                durationBeats = finalDuration,
                velocity = active.velocity
            )
        )
    }

    private fun quantizeBeat(beat: Float): Float {
        if (quantizeGrid == QuantizeGrid.OFF) return beat
        val step = quantizeGrid.beatFraction
        if (step <= 0f) return beat
        return (round(beat / step) * step).coerceAtLeast(0f)
    }

    private fun quantizeDuration(duration: Float): Float {
        if (quantizeGrid == QuantizeGrid.OFF) return duration
        val step = quantizeGrid.beatFraction
        if (step <= 0f) return duration
        val rounded = (round(duration / step) * step)
        return rounded.coerceAtLeast(step)
    }
}

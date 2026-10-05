package com.example.midi

import com.example.audio.InstrumentType

data class MidiNote(
    val id: String = java.util.UUID.randomUUID().toString(),
    val pitch: Int,             // MIDI note number 0-127 (e.g. 60 = Middle C)
    val startBeat: Float,       // Absolute beat position within clip (e.g. 0.0, 0.5, 1.0)
    val durationBeats: Float,   // Length in beats (e.g. 1.0 = quarter note, 0.5 = eighth note)
    val velocity: Float = 0.85f // 0.0 to 1.0
) {
    val noteName: String
        get() {
            val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
            val octave = (pitch / 12) - 1
            val name = noteNames[pitch % 12]
            return "$name$octave"
        }
}

enum class QuantizeGrid(val displayName: String, val beatFraction: Float) {
    OFF("Off", 0f),
    QUARTER("1/4", 1.0f),
    EIGHTH("1/8", 0.5f),
    SIXTEENTH("1/16", 0.25f),
    THIRTY_SECOND("1/32", 0.125f)
}

enum class MusicalScale(val displayName: String, val intervals: List<Int>) {
    MAJOR("Major", listOf(0, 2, 4, 5, 7, 9, 11)),
    NATURAL_MINOR("Minor", listOf(0, 2, 3, 5, 7, 8, 10)),
    HARMONIC_MINOR("Harmonic Minor", listOf(0, 2, 3, 5, 7, 8, 11)),
    DORIAN("Dorian", listOf(0, 2, 3, 5, 7, 9, 10)),
    PENTATONIC_MAJOR("Pentatonic Major", listOf(0, 2, 4, 7, 9)),
    PENTATONIC_MINOR("Pentatonic Minor", listOf(0, 3, 5, 7, 10)),
    BLUES("Blues", listOf(0, 3, 5, 6, 7, 10)),
    CHINESE_PENTATONIC("Chinese Gong (宫)", listOf(0, 2, 4, 7, 9)),
    JAPANESE_HIRAJOSHI("Hirajoshi", listOf(0, 2, 3, 7, 8))
}

data class GeneratedChord(
    val name: String,
    val romanNumeral: String,
    val rootPitch: Int,
    val pitches: List<Int>,
    val durationBeats: Float = 2.0f
)

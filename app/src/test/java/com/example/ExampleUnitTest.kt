package com.example

import com.example.audio.InstrumentPatch
import com.example.audio.InstrumentType
import com.example.midi.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testChordProgressionGenerator() {
        val suggestions = ChordProgressionGenerator.getSuggestedProgressions(
            genre = MusicGenre.POP,
            mood = ChordMood.UPLIFTING
        )
        assertTrue("Should have at least 1 suggestion", suggestions.isNotEmpty())

        val template = suggestions.first()
        val chords = ChordProgressionGenerator.buildChords(
            template = template,
            rootPitch = 60, // C4
            scale = MusicalScale.MAJOR,
            complexity = ChordComplexity.EXTENDED
        )
        assertEquals("Chords count should match degrees", template.degrees.size, chords.size)
        assertTrue("Chords should contain pitches", chords.all { it.pitches.isNotEmpty() })

        val notes = ChordProgressionGenerator.convertToMidiNotes(
            chords = chords,
            startBeat = 0f,
            pattern = VoicingPattern.BLOCK_SUSTAIN
        )
        assertTrue("MidiNotes should be generated", notes.isNotEmpty())
    }

    @Test
    fun testBeatSequencerGrooves() {
        val presets = BeatSequencerDefaults.getPresetPatterns()
        assertTrue("Should have preset drum patterns", presets.isNotEmpty())

        val house = presets[0]
        val midiNotes = BeatSequencerDefaults.patternToMidiNotes(house, 0f, 1)
        assertTrue("Should produce active drum notes", midiNotes.isNotEmpty())
        assertTrue("All notes should have valid pitches", midiNotes.all { it.pitch in 0..127 })
    }

    @Test
    fun testBinaryMidiFileEncoding() {
        val testTracks = listOf(
            StandardMidiHelper.TrackExportData(
                trackName = "Piano Lead",
                instrument = InstrumentType.GRAND_PIANO,
                notes = listOf(
                    MidiNote(pitch = 60, startBeat = 0f, durationBeats = 1f, velocity = 0.8f),
                    MidiNote(pitch = 64, startBeat = 1f, durationBeats = 1f, velocity = 0.85f),
                    MidiNote(pitch = 67, startBeat = 2f, durationBeats = 2f, velocity = 0.9f)
                )
            )
        )

        val midiBytes = StandardMidiHelper.createStandardMidiFile(120, testTracks)
        assertTrue("MIDI bytes should not be empty", midiBytes.isNotEmpty())
        assertEquals("Header should start with MThd", 'M'.code.toByte(), midiBytes[0])
        assertEquals('T'.code.toByte(), midiBytes[1])
        assertEquals('h'.code.toByte(), midiBytes[2])
        assertEquals('d'.code.toByte(), midiBytes[3])
    }

    @Test
    fun testInstrumentPatchDefaults() {
        for (type in InstrumentType.entries) {
            val patch = InstrumentPatch.defaultFor(type)
            assertEquals(type, patch.instrumentType)
            assertTrue(patch.attackMs >= 0f)
            assertTrue(patch.decayMs > 0f)
            assertTrue(patch.sustainLevel in 0f..1f)
            assertTrue(patch.releaseMs > 0f)
            assertTrue(patch.filterCutoff > 0f)
        }
    }

    @Test
    fun testStereoWavExport() {
        val audioEngine = com.example.audio.AudioEngine()
        val tempFile = java.io.File.createTempFile("test_export", ".wav")
        try {
            val success = audioEngine.exportWav(
                outputFile = tempFile,
                totalDurationSeconds = 1.0f,
                bpm = 120,
                normalize = true
            ) {
                listOf(
                    com.example.audio.AudioEngine.ExportNoteEvent(
                        midiNote = 60,
                        startBeat = 0f,
                        durationBeats = 1f,
                        velocity = 0.8f,
                        instrument = InstrumentType.GRAND_PIANO,
                        trackVolume = 0.9f,
                        trackPan = -0.5f // Left panned
                    ),
                    com.example.audio.AudioEngine.ExportNoteEvent(
                        midiNote = 64,
                        startBeat = 0.5f,
                        durationBeats = 1f,
                        velocity = 0.8f,
                        instrument = InstrumentType.ORCHESTRAL_STRINGS,
                        trackVolume = 0.85f,
                        trackPan = 0.5f // Right panned
                    )
                )
            }
            assertTrue("AudioEngine export should succeed", success)
            assertTrue("Output file must exist", tempFile.exists())
            assertTrue("Output file size should be > 44 bytes header", tempFile.length() > 44)

            // Verify WAV header RIFF and WAVE
            val bytes = tempFile.readBytes()
            assertEquals('R'.code.toByte(), bytes[0])
            assertEquals('I'.code.toByte(), bytes[1])
            assertEquals('F'.code.toByte(), bytes[2])
            assertEquals('F'.code.toByte(), bytes[3])
            assertEquals('W'.code.toByte(), bytes[8])
            assertEquals('A'.code.toByte(), bytes[9])
            assertEquals('V'.code.toByte(), bytes[10])
            assertEquals('E'.code.toByte(), bytes[11])
            // Channels = 2 (stereo) at byte offset 22
            assertEquals(2.toByte(), bytes[22])
            assertEquals(0.toByte(), bytes[23])
        } finally {
            tempFile.delete()
        }
    }
}

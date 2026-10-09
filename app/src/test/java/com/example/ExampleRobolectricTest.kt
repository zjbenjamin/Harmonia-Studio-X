package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.InstrumentType
import com.example.midi.MidiNote
import com.example.midi.StandardMidiHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Harmonia Studio", appName)
    }

    @Test
    fun `test json export and parse with android json`() {
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

        val json = StandardMidiHelper.exportProjectToJson(
            title = "Test Song",
            bpm = 128,
            timeSignature = "4/4",
            shareCode = "HRMN-1234",
            tracks = testTracks
        )
        val parsed = StandardMidiHelper.parseProjectFromJson(json)
        assertNotNull(parsed)
        assertEquals("Test Song", parsed?.title)
        assertEquals(128, parsed?.bpm)
        assertEquals(1, parsed?.tracks?.size)
        assertEquals(3, parsed?.tracks?.first()?.notes?.size)
    }
}

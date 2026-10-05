package com.example.midi

import android.util.Log
import com.example.audio.InstrumentType
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.roundToInt

object StandardMidiHelper {

    /**
     * Builds a Standard MIDI File (Format 1 - Multi-track) binary representation.
     */
    fun createStandardMidiFile(
        bpm: Int,
        tracks: List<TrackExportData>
    ): ByteArray {
        val out = ByteArrayOutputStream()

        // 1. Header Chunk: "MThd"
        out.write("MThd".toByteArray(Charsets.US_ASCII))
        writeBigEndianInt(out, 6) // Header size = 6 bytes
        writeBigEndianShort(out, 1) // Format 1: Multi-track synchronous
        writeBigEndianShort(out, tracks.size + 1) // Number of tracks: Tempo track + data tracks
        writeBigEndianShort(out, 480) // 480 ticks per quarter note (PPQN standard)

        // 2. Tempo & Conductor Track (Track 0)
        val tempoTrackStream = ByteArrayOutputStream()
        // Set Tempo meta event: FF 51 03 (microseconds per quarter note)
        val microPerQuarter = 60_000_000 / bpm
        tempoTrackStream.write(0x00) // Delta time 0
        tempoTrackStream.write(0xFF)
        tempoTrackStream.write(0x51)
        tempoTrackStream.write(0x03)
        tempoTrackStream.write((microPerQuarter shr 16) and 0xFF)
        tempoTrackStream.write((microPerQuarter shr 8) and 0xFF)
        tempoTrackStream.write(microPerQuarter and 0xFF)

        // Time Signature: FF 58 04 04 02 18 08 (4/4)
        tempoTrackStream.write(0x00) // Delta 0
        tempoTrackStream.write(0xFF)
        tempoTrackStream.write(0x58)
        tempoTrackStream.write(0x04)
        tempoTrackStream.write(0x04) // Numerator 4
        tempoTrackStream.write(0x02) // Denominator 2^2 = 4
        tempoTrackStream.write(24)   // Clocks per click
        tempoTrackStream.write(8)    // 32nd notes per quarter

        // End of Track meta event: FF 2F 00
        tempoTrackStream.write(0x00)
        tempoTrackStream.write(0xFF)
        tempoTrackStream.write(0x2F)
        tempoTrackStream.write(0x00)

        val tempoBytes = tempoTrackStream.toByteArray()
        out.write("MTrk".toByteArray(Charsets.US_ASCII))
        writeBigEndianInt(out, tempoBytes.size)
        out.write(tempoBytes)

        // 3. Instrument Note Tracks
        for ((channelIdx, track) in tracks.withIndex()) {
            val trackStream = ByteArrayOutputStream()
            val channel = (channelIdx % 16).coerceAtMost(15)

            // Track Name Meta Event: FF 03 len text
            val nameBytes = track.trackName.toByteArray(Charsets.UTF_8)
            trackStream.write(0x00) // Delta 0
            trackStream.write(0xFF)
            trackStream.write(0x03)
            writeVariableLength(trackStream, nameBytes.size)
            trackStream.write(nameBytes)

            // Sort notes into NoteOn and NoteOff events by tick
            val ppqn = 480
            val events = mutableListOf<RawMidiEvent>()
            for (n in track.notes) {
                val startTick = (n.startBeat * ppqn).toLong()
                val endTick = ((n.startBeat + n.durationBeats) * ppqn).toLong()
                val vel = (n.velocity * 127).toInt().coerceIn(1, 127)

                events.add(RawMidiEvent(startTick, true, n.pitch, vel))
                events.add(RawMidiEvent(endTick, false, n.pitch, 0))
            }

            events.sortBy { it.tick }

            var lastTick = 0L
            for (ev in events) {
                val delta = (ev.tick - lastTick).coerceAtLeast(0L)
                writeVariableLength(trackStream, delta.toInt())

                val status = if (ev.isNoteOn) (0x90 or channel) else (0x80 or channel)
                trackStream.write(status)
                trackStream.write(ev.pitch.coerceIn(0, 127))
                trackStream.write(ev.velocity.coerceIn(0, 127))

                lastTick = ev.tick
            }

            // End of track meta event
            trackStream.write(0x00)
            trackStream.write(0xFF)
            trackStream.write(0x2F)
            trackStream.write(0x00)

            val trackBytes = trackStream.toByteArray()
            out.write("MTrk".toByteArray(Charsets.US_ASCII))
            writeBigEndianInt(out, trackBytes.size)
            out.write(trackBytes)
        }

        return out.toByteArray()
    }

    private class RawMidiEvent(
        val tick: Long,
        val isNoteOn: Boolean,
        val pitch: Int,
        val velocity: Int
    )

    private fun writeBigEndianInt(out: ByteArrayOutputStream, value: Int) {
        out.write((value shr 24) and 0xFF)
        out.write((value shr 16) and 0xFF)
        out.write((value shr 8) and 0xFF)
        out.write(value and 0xFF)
    }

    private fun writeBigEndianShort(out: ByteArrayOutputStream, value: Int) {
        out.write((value shr 8) and 0xFF)
        out.write(value and 0xFF)
    }

    private fun writeVariableLength(out: ByteArrayOutputStream, value: Int) {
        var buffer = value and 0x7F
        var v = value ushr 7
        while (v > 0) {
            buffer = (buffer shl 8) or ((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        while (true) {
            out.write(buffer and 0xFF)
            if ((buffer and 0x80) != 0) {
                buffer = buffer ushr 8
            } else {
                break
            }
        }
    }

    data class TrackExportData(
        val trackName: String,
        val instrument: InstrumentType,
        val notes: List<MidiNote>
    )

    /**
     * Cross-platform JSON bundle serialization for cross-device sync
     * (Android, iOS, Web, macOS, Windows).
     */
    fun exportProjectToJson(
        title: String,
        bpm: Int,
        timeSignature: String,
        shareCode: String,
        tracks: List<TrackExportData>
    ): String {
        val root = JSONObject()
        root.put("app", "Harmonia Studio")
        root.put("schemaVersion", 2)
        root.put("title", title)
        root.put("bpm", bpm)
        root.put("timeSignature", timeSignature)
        root.put("shareCode", shareCode)
        root.put("exportedAt", System.currentTimeMillis())

        val tracksArray = JSONArray()
        for (t in tracks) {
            val trackObj = JSONObject()
            trackObj.put("name", t.trackName)
            trackObj.put("instrument", t.instrument.id)

            val notesArray = JSONArray()
            for (n in t.notes) {
                val nObj = JSONObject()
                nObj.put("pitch", n.pitch)
                nObj.put("start", n.startBeat)
                nObj.put("dur", n.durationBeats)
                nObj.put("vel", n.velocity)
                notesArray.put(nObj)
            }
            trackObj.put("notes", notesArray)
            tracksArray.put(trackObj)
        }
        root.put("tracks", tracksArray)

        return root.toString(2)
    }

    fun parseProjectFromJson(jsonString: String): ParsedProjectBundle? {
        return try {
            val root = JSONObject(jsonString)
            val title = root.optString("title", "Imported Project")
            val bpm = root.optInt("bpm", 120)
            val timeSignature = root.optString("timeSignature", "4/4")
            val shareCode = root.optString("shareCode", "HRMN-SYNC")

            val tracks = mutableListOf<TrackExportData>()
            val tracksArray = root.optJSONArray("tracks") ?: JSONArray()
            for (i in 0 until tracksArray.length()) {
                val tObj = tracksArray.getJSONObject(i)
                val trackName = tObj.optString("name", "Track ${i + 1}")
                val instId = tObj.optString("instrument", "grand_piano")
                val instrument = InstrumentType.fromId(instId)

                val notes = mutableListOf<MidiNote>()
                val notesArray = tObj.optJSONArray("notes") ?: JSONArray()
                for (j in 0 until notesArray.length()) {
                    val nObj = notesArray.getJSONObject(j)
                    notes.add(
                        MidiNote(
                            pitch = nObj.getInt("pitch"),
                            startBeat = nObj.getDouble("start").toFloat(),
                            durationBeats = nObj.getDouble("dur").toFloat(),
                            velocity = nObj.optDouble("vel", 0.85).toFloat()
                        )
                    )
                }
                tracks.add(TrackExportData(trackName, instrument, notes))
            }

            ParsedProjectBundle(title, bpm, timeSignature, shareCode, tracks)
        } catch (e: Exception) {
            Log.e("StandardMidiHelper", "Failed to parse project JSON", e)
            null
        }
    }

    data class ParsedProjectBundle(
        val title: String,
        val bpm: Int,
        val timeSignature: String,
        val shareCode: String,
        val tracks: List<TrackExportData>
    )
}

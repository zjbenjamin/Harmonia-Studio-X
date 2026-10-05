package com.example.midi

import androidx.compose.ui.graphics.Color
import com.example.audio.InstrumentType
import kotlin.math.roundToInt
import kotlin.random.Random

data class DrumStep(
    val active: Boolean = false,
    val velocity: Float = 0.85f, // 0.0 to 1.0
    val rollSubdivisions: Int = 1 // 1 = normal, 2 = flam/roll, 3 = triplet ratchets
)

data class DrumLane(
    val id: String,
    val name: String,
    val midiNote: Int,
    val color: Color,
    val steps: List<DrumStep>,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val instrumentType: InstrumentType = InstrumentType.DRUM_KIT,
    val noteName: String = "",
    val octave: Int = 3
) {
    val isSynth: Boolean
        get() = instrumentType != InstrumentType.DRUM_KIT

    val displayNote: String
        get() = if (noteName.isNotBlank()) noteName else {
            val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
            val oct = (midiNote / 12) - 1
            val n = noteNames[(midiNote % 12 + 12) % 12]
            "$n$oct"
        }
}

data class BeatPattern(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Main Groove",
    val stepCount: Int = 16,
    val swingPercent: Int = 0, // 0 to 75%
    val lanes: List<DrumLane>
)

object BeatSequencerDefaults {

    fun createEmptySynthLanes(stepCount: Int = 16): List<DrumLane> {
        return listOf(
            DrumLane(
                id = "synth_bass",
                name = "Acid 303 Bass",
                midiNote = 36, // C2
                noteName = "C2",
                instrumentType = InstrumentType.ACID_303_BASS,
                color = Color(0xFFEF4444),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "synth_sub",
                name = "Sub 808 Bass",
                midiNote = 38, // D2
                noteName = "D2",
                instrumentType = InstrumentType.SUB_808_BASS,
                color = Color(0xFFDC2626),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "synth_lead",
                name = "Cyber Lead Synth",
                midiNote = 60, // C4
                noteName = "C4",
                instrumentType = InstrumentType.SYNTH_POLY_KEYS,
                color = Color(0xFF06B6D4),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "synth_lead_high",
                name = "Hyper Lead High",
                midiNote = 67, // G4
                noteName = "G4",
                instrumentType = InstrumentType.SYNTH_POLY_KEYS,
                color = Color(0xFF38BDF8),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "synth_pluck",
                name = "Neon Harp Arp",
                midiNote = 64, // E4
                noteName = "E4",
                instrumentType = InstrumentType.CELTIC_HARP,
                color = Color(0xFFA855F7),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "synth_pad",
                name = "Cosmic String Pad",
                midiNote = 57, // A3
                noteName = "A3",
                instrumentType = InstrumentType.ORCHESTRAL_STRINGS,
                color = Color(0xFF818CF8),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "synth_keys",
                name = "Rhodes E-Piano",
                midiNote = 60, // C4
                noteName = "C4",
                instrumentType = InstrumentType.ELECTRIC_PIANO,
                color = Color(0xFFF59E0B),
                steps = List(stepCount) { DrumStep(active = false) }
            )
        )
    }

    fun createEmptyDrumLanes(stepCount: Int = 16): List<DrumLane> {
        return listOf(
            DrumLane(
                id = "kick",
                name = "Kick (808)",
                midiNote = 36,
                instrumentType = InstrumentType.DRUM_KIT,
                color = Color(0xFFF43F5E),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "snare",
                name = "Snare Drum",
                midiNote = 38,
                instrumentType = InstrumentType.DRUM_KIT,
                color = Color(0xFFFB923C),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "hihat_closed",
                name = "Closed Hi-Hat",
                midiNote = 42,
                instrumentType = InstrumentType.DRUM_KIT,
                color = Color(0xFF38BDF8),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "hihat_open",
                name = "Open Hi-Hat",
                midiNote = 46,
                instrumentType = InstrumentType.DRUM_KIT,
                color = Color(0xFF6366F1),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "clap",
                name = "Hand Clap",
                midiNote = 39,
                instrumentType = InstrumentType.DRUM_KIT,
                color = Color(0xFFC084FC),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "tom_low",
                name = "Low Tom",
                midiNote = 41,
                instrumentType = InstrumentType.DRUM_KIT,
                color = Color(0xFF10B981),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "tom_high",
                name = "High Tom",
                midiNote = 48,
                instrumentType = InstrumentType.DRUM_KIT,
                color = Color(0xFF34D399),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "cowbell",
                name = "Cowbell 808",
                midiNote = 56,
                instrumentType = InstrumentType.DRUM_KIT,
                color = Color(0xFFEAB308),
                steps = List(stepCount) { DrumStep(active = false) }
            )
        )
    }

    fun createEmptyLanes(stepCount: Int = 16): List<DrumLane> {
        return createEmptySynthLanes(stepCount) + createEmptyDrumLanes(stepCount)
    }

    /**
     * Converts a BeatPattern into a list of MidiNote objects with precise
     * swing / groove quantization offset applied.
     */
    fun patternToMidiNotes(
        pattern: BeatPattern,
        startBeat: Float = 0f,
        totalBars: Int = 1,
        onlyDrums: Boolean = false,
        onlySynths: Boolean = false
    ): List<MidiNote> {
        val notes = mutableListOf<MidiNote>()
        val stepDurationBeats = 4.0f / pattern.stepCount // 0.25 for 16th notes
        val swingFraction = (pattern.swingPercent / 100f) * 0.16f // Swing offset on even steps

        val anySolo = pattern.lanes.any { it.isSolo }

        for (bar in 0 until totalBars) {
            val barOffset = startBeat + (bar * 4.0f)

            for (lane in pattern.lanes) {
                if (lane.isMuted) continue
                if (anySolo && !lane.isSolo) continue
                if (onlyDrums && lane.isSynth) continue
                if (onlySynths && !lane.isSynth) continue

                for (stepIdx in 0 until pattern.stepCount) {
                    val step = lane.steps.getOrNull(stepIdx) ?: continue
                    if (!step.active) continue

                    // Calculate swing offset on off-beat steps (1, 3, 5, 7, etc. in 16ths)
                    val isOffBeat = (stepIdx % 2) != 0
                    val swingOffset = if (isOffBeat) swingFraction else 0f
                    val noteStart = barOffset + (stepIdx * stepDurationBeats) + swingOffset

                    if (step.rollSubdivisions > 1) {
                        // Ratchet / Roll burst
                        val subDuration = stepDurationBeats / step.rollSubdivisions
                        for (r in 0 until step.rollSubdivisions) {
                            notes.add(
                                MidiNote(
                                    pitch = lane.midiNote,
                                    startBeat = noteStart + (r * subDuration),
                                    durationBeats = subDuration * 0.8f,
                                    velocity = step.velocity * (0.85f + r * 0.05f)
                                )
                            )
                        }
                    } else {
                        notes.add(
                            MidiNote(
                                pitch = lane.midiNote,
                                startBeat = noteStart,
                                durationBeats = if (lane.isSynth) stepDurationBeats * 0.95f else stepDurationBeats * 0.75f,
                                velocity = step.velocity
                            )
                        )
                    }
                }
            }
        }

        return notes
    }

    fun getPresetPatterns(): List<BeatPattern> {
        return listOf(
            createCyberSynthwavePreset(),
            createAcidTechnoPreset(),
            createLoFiChillPreset(),
            createTrap808Preset(),
            createHouseFourOnTheFloor()
        )
    }

    private fun createCyberSynthwavePreset(): BeatPattern {
        val lanes = createEmptyLanes(16).map { lane ->
            when (lane.id) {
                // Synth tracks
                "synth_bass" -> lane.copy(
                    steps = List(16) { idx ->
                        // Pulsing eighth notes
                        DrumStep(active = idx % 2 == 0, velocity = if (idx % 4 == 0) 0.95f else 0.75f)
                    }
                )
                "synth_lead" -> lane.copy(
                    steps = List(16) { idx ->
                        // Hook melody stabs
                        DrumStep(active = idx == 0 || idx == 6 || idx == 10 || idx == 14, velocity = 0.9f)
                    }
                )
                "synth_pluck" -> lane.copy(
                    steps = List(16) { idx ->
                        // Arp counter-rhythm
                        DrumStep(active = idx == 2 || idx == 4 || idx == 8 || idx == 11, velocity = 0.85f)
                    }
                )
                "synth_pad" -> lane.copy(
                    steps = List(16) { idx ->
                        // Downbeat anchor
                        DrumStep(active = idx == 0 || idx == 8, velocity = 0.7f)
                    }
                )
                // Drum tracks
                "kick" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx % 4 == 0, velocity = 0.98f)
                    }
                )
                "snare" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 4 || idx == 12, velocity = 0.92f)
                    }
                )
                "hihat_closed" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx % 2 != 0, velocity = if (idx % 4 == 2) 0.8f else 0.55f)
                    }
                )
                "hihat_open" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 2 || idx == 10 || idx == 14, velocity = 0.75f)
                    }
                )
                "clap" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 4 || idx == 12, velocity = 0.88f)
                    }
                )
                else -> lane
            }
        }
        return BeatPattern(name = "Cyber Synthwave & Bass", swingPercent = 10, lanes = lanes)
    }

    private fun createAcidTechnoPreset(): BeatPattern {
        val lanes = createEmptyLanes(16).map { lane ->
            when (lane.id) {
                "synth_bass" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(
                            active = idx == 0 || idx == 2 || idx == 3 || idx == 6 || idx == 8 || idx == 11 || idx == 14,
                            velocity = if (idx == 0 || idx == 6 || idx == 14) 1.0f else 0.75f
                        )
                    }
                )
                "synth_lead" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 3 || idx == 7 || idx == 11 || idx == 15, velocity = 0.88f)
                    }
                )
                "synth_pluck" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 1 || idx == 5 || idx == 9 || idx == 13, velocity = 0.8f)
                    }
                )
                "kick" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx % 4 == 0, velocity = 1.0f)
                    }
                )
                "snare" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 4 || idx == 12, velocity = 0.95f)
                    }
                )
                "hihat_open" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 2 || idx == 6 || idx == 10 || idx == 14, velocity = 0.85f)
                    }
                )
                "hihat_closed" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = true, velocity = 0.6f)
                    }
                )
                "tom_low" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 15, velocity = 0.85f)
                    }
                )
                else -> lane
            }
        }
        return BeatPattern(name = "Acid Techno Arp & Drive", swingPercent = 0, lanes = lanes)
    }

    private fun createLoFiChillPreset(): BeatPattern {
        val lanes = createEmptyLanes(16).map { lane ->
            when (lane.id) {
                "synth_pad" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 0 || idx == 8, velocity = 0.8f)
                    }
                )
                "synth_keys" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 2 || idx == 6 || idx == 10 || idx == 14, velocity = 0.75f)
                    }
                )
                "synth_sub" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 0 || idx == 6 || idx == 10, velocity = 0.9f)
                    }
                )
                "kick" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 0 || idx == 5 || idx == 10, velocity = 0.92f)
                    }
                )
                "snare" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 4 || idx == 12, velocity = 0.9f)
                    }
                )
                "hihat_closed" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = true, velocity = if (idx % 2 == 0) 0.8f else 0.5f)
                    }
                )
                "cowbell" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 7 || idx == 15, velocity = 0.65f)
                    }
                )
                else -> lane
            }
        }
        return BeatPattern(name = "Lo-Fi Chill & Poly Keys", swingPercent = 48, lanes = lanes)
    }

    private fun createTrap808Preset(): BeatPattern {
        val lanes = createEmptyLanes(16).map { lane ->
            when (lane.id) {
                "synth_sub" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 0 || idx == 6 || idx == 10, velocity = 1.0f)
                    }
                )
                "synth_pluck" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 0 || idx == 3 || idx == 7 || idx == 12 || idx == 14, velocity = 0.85f)
                    }
                )
                "kick" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 0 || idx == 6 || idx == 10, velocity = 1.0f)
                    }
                )
                "snare" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 8, velocity = 0.95f)
                    }
                )
                "clap" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 8, velocity = 0.92f)
                    }
                )
                "hihat_closed" -> lane.copy(
                    steps = List(16) { idx ->
                        val isRoll = idx == 14 || idx == 15
                        DrumStep(
                            active = true,
                            velocity = if (idx % 2 == 0) 0.85f else 0.55f,
                            rollSubdivisions = if (isRoll) 3 else 1
                        )
                    }
                )
                "hihat_open" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 4 || idx == 12, velocity = 0.75f)
                    }
                )
                else -> lane
            }
        }
        return BeatPattern(name = "Modern Trap 808 & Dark Arp", swingPercent = 0, lanes = lanes)
    }

    private fun createHouseFourOnTheFloor(): BeatPattern {
        val lanes = createEmptyLanes(16).map { lane ->
            when (lane.id) {
                "synth_bass" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx % 2 != 0, velocity = 0.85f)
                    }
                )
                "synth_keys" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 2 || idx == 6 || idx == 10 || idx == 14, velocity = 0.85f)
                    }
                )
                "kick" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx % 4 == 0, velocity = 0.95f)
                    }
                )
                "snare" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 4 || idx == 12, velocity = 0.9f)
                    }
                )
                "hihat_closed" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx % 2 != 0, velocity = if (idx % 4 == 2) 0.85f else 0.6f)
                    }
                )
                "hihat_open" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 2 || idx == 6 || idx == 10 || idx == 14, velocity = 0.8f)
                    }
                )
                "clap" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 4 || idx == 12, velocity = 0.88f)
                    }
                )
                else -> lane
            }
        }
        return BeatPattern(name = "Classic 4-on-Floor House", swingPercent = 15, lanes = lanes)
    }
}

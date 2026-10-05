package com.example.midi

import androidx.compose.ui.graphics.Color
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
    val isSolo: Boolean = false
)

data class BeatPattern(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Main Groove",
    val stepCount: Int = 16,
    val swingPercent: Int = 0, // 0 to 75%
    val lanes: List<DrumLane>
)

object BeatSequencerDefaults {

    fun createEmptyLanes(stepCount: Int = 16): List<DrumLane> {
        return listOf(
            DrumLane(
                id = "kick",
                name = "Kick (808)",
                midiNote = 36,
                color = Color(0xFFEF4444),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "snare",
                name = "Snare Drum",
                midiNote = 38,
                color = Color(0xFFF97316),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "hihat_closed",
                name = "Closed Hi-Hat",
                midiNote = 42,
                color = Color(0xFF38BDF8),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "hihat_open",
                name = "Open Hi-Hat",
                midiNote = 46,
                color = Color(0xFF818CF8),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "clap",
                name = "Hand Clap",
                midiNote = 39,
                color = Color(0xFFA855F7),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "tom_low",
                name = "Low Tom",
                midiNote = 41,
                color = Color(0xFF10B981),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "tom_high",
                name = "High Tom",
                midiNote = 48,
                color = Color(0xFF34D399),
                steps = List(stepCount) { DrumStep(active = false) }
            ),
            DrumLane(
                id = "cowbell",
                name = "Cowbell 808",
                midiNote = 56,
                color = Color(0xFFF59E0B),
                steps = List(stepCount) { DrumStep(active = false) }
            )
        )
    }

    /**
     * Converts a BeatPattern into a list of MidiNote objects with precise
     * swing / groove quantization offset applied.
     */
    fun patternToMidiNotes(
        pattern: BeatPattern,
        startBeat: Float = 0f,
        totalBars: Int = 1
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
                                durationBeats = stepDurationBeats * 0.75f,
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
            createHouseFourOnTheFloor(),
            createTrap808Groove(),
            createBoomBapLoFiGroove(),
            createSynthwaveDriveGroove(),
            createFunkBreakbeat()
        )
    }

    private fun createHouseFourOnTheFloor(): BeatPattern {
        val lanes = createEmptyLanes(16).map { lane ->
            when (lane.id) {
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

    private fun createTrap808Groove(): BeatPattern {
        val lanes = createEmptyLanes(16).map { lane ->
            when (lane.id) {
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
        return BeatPattern(name = "Modern 808 Trap Roll", swingPercent = 0, lanes = lanes)
    }

    private fun createBoomBapLoFiGroove(): BeatPattern {
        val lanes = createEmptyLanes(16).map { lane ->
            when (lane.id) {
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
        return BeatPattern(name = "90s Boom-Bap & Lo-Fi", swingPercent = 45, lanes = lanes)
    }

    private fun createSynthwaveDriveGroove(): BeatPattern {
        val lanes = createEmptyLanes(16).map { lane ->
            when (lane.id) {
                "kick" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx % 4 == 0, velocity = 0.98f)
                    }
                )
                "snare" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 4 || idx == 12, velocity = 0.95f)
                    }
                )
                "hihat_closed" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = true, velocity = if (idx % 2 == 0) 0.75f else 0.6f)
                    }
                )
                "tom_low" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 14, velocity = 0.85f)
                    }
                )
                "tom_high" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 15, velocity = 0.85f)
                    }
                )
                else -> lane
            }
        }
        return BeatPattern(name = "Retro Synthwave Drive", swingPercent = 10, lanes = lanes)
    }

    private fun createFunkBreakbeat(): BeatPattern {
        val lanes = createEmptyLanes(16).map { lane ->
            when (lane.id) {
                "kick" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 0 || idx == 6 || idx == 10 || idx == 13, velocity = 0.9f)
                    }
                )
                "snare" -> lane.copy(
                    steps = List(16) { idx ->
                        val isGhost = idx == 7 || idx == 11
                        val isMain = idx == 4 || idx == 12
                        DrumStep(active = isMain || isGhost, velocity = if (isMain) 0.95f else 0.45f)
                    }
                )
                "hihat_closed" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = true, velocity = 0.7f)
                    }
                )
                "cowbell" -> lane.copy(
                    steps = List(16) { idx ->
                        DrumStep(active = idx == 2 || idx == 8 || idx == 14, velocity = 0.7f)
                    }
                )
                else -> lane
            }
        }
        return BeatPattern(name = "Syncopated Funk Break", swingPercent = 35, lanes = lanes)
    }
}

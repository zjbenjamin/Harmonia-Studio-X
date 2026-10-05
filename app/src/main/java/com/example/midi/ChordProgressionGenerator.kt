package com.example.midi

import kotlin.math.max

enum class MusicGenre(val displayName: String, val description: String) {
    POP("Pop & Chart", "Catchy anthemic progressions with strong resolution"),
    LO_FI_RNB("Lo-Fi & Neo-Soul", "Warm lush 7ths and 9ths with mellow melancholy"),
    JAZZ("Jazz & Fusion", "Sophisticated ii-V-I progressions with extended voice leading"),
    EDM_HOUSE("EDM & Dance", "High-energy festival drops, euphoric builds, and modal tension"),
    CINEMATIC("Cinematic & Epic", "Grand orchestral emotional drama, wide dynamic swells"),
    SYNTHWAVE("80s Synthwave", "Retro-futuristic minor mode progressions with analog nostalgia"),
    ROCK_INDIE("Rock & Alternative", "Powerful guitar-driven riffs and modal hooks"),
    ORIENTAL("GuFeng Traditional", "Traditional pentatonic modal elegance for Guzheng and Erhu")
}

enum class ChordMood(val displayName: String, val emoji: String) {
    UPLIFTING("Uplifting & Bright", "☀️"),
    MELANCHOLIC("Melancholic & Deep", "🌧️"),
    DARK_TENSE("Dark & Mysterious", "🌑"),
    DREAMY("Dreamy & Chill", "✨"),
    EPIC("Epic & Triumphant", "⚔️")
}

enum class ChordComplexity(val displayName: String, val description: String) {
    SIMPLE("Basic Triads", "Pure fundamental 3-note chords"),
    EXTENDED("Extended (7ths & 9ths)", "Adds maj7, min7, dom7, add9 depth"),
    ADVANCED("Jazz & Neo-Soul (11ths/Sus/Alt)", "Rich sus4, 11th, dim7, and voice alterations")
}

enum class VoicingPattern(val displayName: String) {
    BLOCK_SUSTAIN("Sustained Chords"),
    RHYTHMIC_PULSE("Rhythmic Pulse (1/4 Note)"),
    ARPEGGIO_UP("Arpeggiated Up"),
    BALLAD_STRUM("Ballad Strum")
}

data class ProgressionTemplate(
    val title: String,
    val genre: MusicGenre,
    val mood: ChordMood,
    val degrees: List<Int>, // Scale degrees 1-based (e.g. 1, 5, 6, 4)
    val chordTypes: List<String>, // "maj", "min", "7", "maj7", "m7", "dim", "sus4", "m9", "maj9"
    val romanNumerals: List<String>,
    val description: String
)

object ChordProgressionGenerator {

    val ROOT_NOTES = listOf(
        "C" to 60,
        "C#" to 61,
        "D" to 62,
        "D#" to 63,
        "E" to 64,
        "F" to 65,
        "F#" to 66,
        "G" to 67,
        "G#" to 68,
        "A" to 69,
        "A#" to 70,
        "B" to 71
    )

    private val PRESET_TEMPLATES = listOf(
        // POP
        ProgressionTemplate(
            title = "Axis of Awesome (Hit Maker)",
            genre = MusicGenre.POP,
            mood = ChordMood.UPLIFTING,
            degrees = listOf(1, 5, 6, 4),
            chordTypes = listOf("maj", "maj", "min", "maj"),
            romanNumerals = listOf("I", "V", "vi", "IV"),
            description = "The most iconic progression in modern pop history (Someone Like You, Don't Stop Believin')"
        ),
        ProgressionTemplate(
            title = "Emotional Pop Anthem",
            genre = MusicGenre.POP,
            mood = ChordMood.MELANCHOLIC,
            degrees = listOf(6, 4, 1, 5),
            chordTypes = listOf("min", "maj", "maj", "maj"),
            romanNumerals = listOf("vi", "IV", "I", "V"),
            description = "Heartfelt, dramatic pop flow used in countless radio hits"
        ),
        ProgressionTemplate(
            title = "Euphoric Daylight",
            genre = MusicGenre.POP,
            mood = ChordMood.DREAMY,
            degrees = listOf(1, 6, 4, 5),
            chordTypes = listOf("maj", "min", "maj", "maj"),
            romanNumerals = listOf("I", "vi", "IV", "V"),
            description = "50s Doo-Wop nostalgic progression with modern pop sheen"
        ),

        // LO-FI & NEO-SOUL
        ProgressionTemplate(
            title = "Midnight Coffee Study",
            genre = MusicGenre.LO_FI_RNB,
            mood = ChordMood.DREAMY,
            degrees = listOf(2, 5, 1, 6),
            chordTypes = listOf("m7", "7", "maj7", "m7"),
            romanNumerals = listOf("ii7", "V7", "Imaj7", "vi7"),
            description = "Lush chillhop chords with classic vinyl warmth"
        ),
        ProgressionTemplate(
            title = "Rainy Bedroom Neo-Soul",
            genre = MusicGenre.LO_FI_RNB,
            mood = ChordMood.MELANCHOLIC,
            degrees = listOf(4, 3, 2, 1),
            chordTypes = listOf("maj7", "m7", "m7", "maj7"),
            romanNumerals = listOf("IVmaj7", "iii7", "ii7", "Imaj7"),
            description = "Melancholy stepwise descent loved by J Dilla and Tom Misch"
        ),
        ProgressionTemplate(
            title = "Velvet Sunset R&B",
            genre = MusicGenre.LO_FI_RNB,
            mood = ChordMood.UPLIFTING,
            degrees = listOf(1, 4, 2, 5),
            chordTypes = listOf("maj9", "maj7", "m9", "7sus4"),
            romanNumerals = listOf("Imaj9", "IVmaj7", "ii9", "V7sus4"),
            description = "Silky smooth contemporary R&B ballad groove"
        ),

        // JAZZ
        ProgressionTemplate(
            title = "Autumn Leaves 2-5-1 Cycle",
            genre = MusicGenre.JAZZ,
            mood = ChordMood.MELANCHOLIC,
            degrees = listOf(2, 5, 1, 4),
            chordTypes = listOf("m7", "7", "maj7", "maj7"),
            romanNumerals = listOf("ii7", "V7", "Imaj7", "IVmaj7"),
            description = "Essential jazz standard circle-of-fifths harmonic movement"
        ),
        ProgressionTemplate(
            title = "Midnight Blue Minor ii-V-i",
            genre = MusicGenre.JAZZ,
            mood = ChordMood.DARK_TENSE,
            degrees = listOf(2, 5, 1, 6),
            chordTypes = listOf("m7b5", "7alt", "m7", "7"),
            romanNumerals = listOf("iiø7", "V7alt", "i7", "VI7"),
            description = "Sophisticated modal jazz cadence with dark tension"
        ),
        ProgressionTemplate(
            title = "Bossa Nova Breeze",
            genre = MusicGenre.JAZZ,
            mood = ChordMood.DREAMY,
            degrees = listOf(1, 6, 2, 5),
            chordTypes = listOf("maj7", "7", "m7", "7"),
            romanNumerals = listOf("Imaj7", "VI7", "ii7", "V7"),
            description = "Girl from Ipanema style swaying Latin jazz progression"
        ),

        // EDM & DANCE
        ProgressionTemplate(
            title = "Festival Anthem Build",
            genre = MusicGenre.EDM_HOUSE,
            mood = ChordMood.EPIC,
            degrees = listOf(6, 7, 1, 3),
            chordTypes = listOf("min", "maj", "min", "maj"),
            romanNumerals = listOf("i", "VII", "VI", "III"),
            description = "High energy festival progressive house driver (Avicii style)"
        ),
        ProgressionTemplate(
            title = "Deep Club Groove",
            genre = MusicGenre.EDM_HOUSE,
            mood = ChordMood.DARK_TENSE,
            degrees = listOf(1, 6, 7, 1),
            chordTypes = listOf("min", "maj", "maj", "min"),
            romanNumerals = listOf("i", "VI", "VII", "i"),
            description = "Deep house hypnotic chord stab sequence"
        ),

        // CINEMATIC
        ProgressionTemplate(
            title = "Hans Zimmer Epic Hero",
            genre = MusicGenre.CINEMATIC,
            mood = ChordMood.EPIC,
            degrees = listOf(1, 6, 3, 7),
            chordTypes = listOf("min", "maj", "maj", "maj"),
            romanNumerals = listOf("i", "VI", "III", "VII"),
            description = "Gladiator & Inception scale grandeur for heavy brass and strings"
        ),
        ProgressionTemplate(
            title = "Hope Across the Stars",
            genre = MusicGenre.CINEMATIC,
            mood = ChordMood.UPLIFTING,
            degrees = listOf(1, 4, 6, 5),
            chordTypes = listOf("maj", "maj", "min", "sus4"),
            romanNumerals = listOf("I", "IV", "vi", "Vsus4"),
            description = "Awe-inspiring orchestral resolve with emotional suspended lift"
        ),
        ProgressionTemplate(
            title = "Tragic Requiem",
            genre = MusicGenre.CINEMATIC,
            mood = ChordMood.DARK_TENSE,
            degrees = listOf(1, 4, 5, 1),
            chordTypes = listOf("min", "min", "maj", "min"),
            romanNumerals = listOf("i", "iv", "V", "i"),
            description = "Deep classical pathos with harmonic minor dominant V chord"
        ),

        // SYNTHWAVE
        ProgressionTemplate(
            title = "Neon Highway 1984",
            genre = MusicGenre.SYNTHWAVE,
            mood = ChordMood.DREAMY,
            degrees = listOf(6, 4, 5, 6),
            chordTypes = listOf("min", "maj", "maj", "min"),
            romanNumerals = listOf("vi", "IV", "V", "vi"),
            description = "Dusk driving on the coastal highway with analog pads"
        ),
        ProgressionTemplate(
            title = "Cyberpunk Outrun",
            genre = MusicGenre.SYNTHWAVE,
            mood = ChordMood.DARK_TENSE,
            degrees = listOf(1, 6, 4, 5),
            chordTypes = listOf("min", "maj", "min", "maj"),
            romanNumerals = listOf("i", "VI", "iv", "v"),
            description = "Dark retro-electro synth arpeggio bed"
        ),

        // ROCK & INDIE
        ProgressionTemplate(
            title = "Grunge & Alternative Anthem",
            genre = MusicGenre.ROCK_INDIE,
            mood = ChordMood.MELANCHOLIC,
            degrees = listOf(1, 3, 4, 6),
            chordTypes = listOf("min", "maj", "min", "maj"),
            romanNumerals = listOf("i", "III", "iv", "VI"),
            description = "90s alt-rock distorted power chord drive"
        ),

        // ORIENTAL / GUFENG
        ProgressionTemplate(
            title = "Spring River Flowers (春江花月)",
            genre = MusicGenre.ORIENTAL,
            mood = ChordMood.DREAMY,
            degrees = listOf(1, 5, 6, 1),
            chordTypes = listOf("sus2", "maj", "min7", "sus2"),
            romanNumerals = listOf("Gong", "Zhi", "Yu", "Gong"),
            description = "Pentatonic Chinese palace aesthetic, ideal for Guzheng & Erhu"
        ),
        ProgressionTemplate(
            title = "Mountain Cloud Mist (云水禅心)",
            genre = MusicGenre.ORIENTAL,
            mood = ChordMood.UPLIFTING,
            degrees = listOf(6, 1, 2, 5),
            chordTypes = listOf("min7", "maj7", "m7", "sus4"),
            romanNumerals = listOf("Yu", "Gong", "Shang", "Zhi"),
            description = "Flowing meditative oriental harmonics with crystalline beauty"
        )
    )

    fun getSuggestedProgressions(
        genre: MusicGenre,
        mood: ChordMood
    ): List<ProgressionTemplate> {
        val matches = PRESET_TEMPLATES.filter { it.genre == genre && it.mood == mood }
        if (matches.isNotEmpty()) return matches

        // Fallback to genre matches or mood matches
        val genreMatches = PRESET_TEMPLATES.filter { it.genre == genre }
        if (genreMatches.isNotEmpty()) return genreMatches

        return PRESET_TEMPLATES.filter { it.mood == mood }.ifEmpty { PRESET_TEMPLATES.take(3) }
    }

    /**
     * Generate concrete chord objects with actual MIDI pitch numbers based on
     * the root key, template, and complexity.
     */
    fun buildChords(
        template: ProgressionTemplate,
        rootPitch: Int, // e.g. 60 for C4
        scale: MusicalScale,
        complexity: ChordComplexity,
        octave: Int = 4
    ): List<GeneratedChord> {
        val basePitch = (rootPitch % 12) + (octave * 12)
        val result = mutableListOf<GeneratedChord>()

        for (i in template.degrees.indices) {
            val degree = template.degrees[i] // 1-based
            val baseType = template.chordTypes[i]
            val roman = template.romanNumerals.getOrElse(i) { "I" }

            // Calculate root note for this degree using the selected scale
            val intervalFromRoot = getIntervalForDegree(degree, scale)
            val chordRoot = basePitch + intervalFromRoot

            // Build pitch list based on complexity
            val pitches = constructChordPitches(chordRoot, baseType, complexity)
            val chordName = getChordDisplayName(chordRoot, baseType, complexity)

            result.add(
                GeneratedChord(
                    name = chordName,
                    romanNumeral = roman,
                    rootPitch = chordRoot,
                    pitches = pitches,
                    durationBeats = 2.0f
                )
            )
        }

        return result
    }

    /**
     * Convert chord progression to list of MidiNote events formatted with the
     * chosen voicing pattern and starting beat.
     */
    fun convertToMidiNotes(
        chords: List<GeneratedChord>,
        startBeat: Float,
        pattern: VoicingPattern,
        velocity: Float = 0.82f
    ): List<MidiNote> {
        val notes = mutableListOf<MidiNote>()
        var currentBeat = startBeat

        for (chord in chords) {
            val duration = chord.durationBeats

            when (pattern) {
                VoicingPattern.BLOCK_SUSTAIN -> {
                    // All chord pitches played simultaneously and sustained
                    for (pitch in chord.pitches) {
                        notes.add(
                            MidiNote(
                                pitch = pitch,
                                startBeat = currentBeat,
                                durationBeats = duration * 0.95f,
                                velocity = velocity
                            )
                        )
                    }
                    // Add bass octave root note for depth
                    notes.add(
                        MidiNote(
                            pitch = chord.rootPitch - 12,
                            startBeat = currentBeat,
                            durationBeats = duration * 0.95f,
                            velocity = velocity * 0.95f
                        )
                    )
                }

                VoicingPattern.RHYTHMIC_PULSE -> {
                    // Split duration into quarter-note pulses (1 beat each)
                    val pulseCount = max(1, duration.toInt())
                    for (p in 0 until pulseCount) {
                        val pulseBeat = currentBeat + p
                        for (pitch in chord.pitches) {
                            notes.add(
                                MidiNote(
                                    pitch = pitch,
                                    startBeat = pulseBeat,
                                    durationBeats = 0.8f,
                                    velocity = if (p == 0) velocity else velocity * 0.8f
                                )
                            )
                        }
                        notes.add(
                            MidiNote(
                                pitch = chord.rootPitch - 12,
                                startBeat = pulseBeat,
                                durationBeats = 0.85f,
                                velocity = velocity * 0.9f
                            )
                        )
                    }
                }

                VoicingPattern.ARPEGGIO_UP -> {
                    // Step through each pitch ascending across 8th notes (0.5 beats)
                    val sortedPitches = chord.pitches.sorted()
                    val step = duration / max(4, sortedPitches.size)
                    for (idx in sortedPitches.indices) {
                        notes.add(
                            MidiNote(
                                pitch = sortedPitches[idx],
                                startBeat = currentBeat + (idx * step),
                                durationBeats = step * 1.5f,
                                velocity = velocity * (0.85f + (idx * 0.05f))
                            )
                        )
                    }
                    // Grounding bass note
                    notes.add(
                        MidiNote(
                            pitch = chord.rootPitch - 12,
                            startBeat = currentBeat,
                            durationBeats = duration * 0.9f,
                            velocity = velocity * 0.95f
                        )
                    )
                }

                VoicingPattern.BALLAD_STRUM -> {
                    // Micro-stagger notes by 0.04 beats to emulate gentle hand strumming
                    for ((idx, pitch) in chord.pitches.withIndex()) {
                        val offset = idx * 0.04f
                        notes.add(
                            MidiNote(
                                pitch = pitch,
                                startBeat = currentBeat + offset,
                                durationBeats = (duration - offset) * 0.95f,
                                velocity = velocity * (0.75f + idx * 0.08f)
                            )
                        )
                    }
                    notes.add(
                        MidiNote(
                            pitch = chord.rootPitch - 12,
                            startBeat = currentBeat,
                            durationBeats = duration * 0.95f,
                            velocity = velocity * 0.9f
                        )
                    )
                }
            }

            currentBeat += duration
        }

        return notes
    }

    private fun getIntervalForDegree(degree: Int, scale: MusicalScale): Int {
        val clampedDegree = ((degree - 1) % scale.intervals.size).coerceAtLeast(0)
        return scale.intervals[clampedDegree]
    }

    private fun constructChordPitches(
        root: Int,
        baseType: String,
        complexity: ChordComplexity
    ): List<Int> {
        val list = mutableListOf<Int>()
        list.add(root)

        val isMinor = baseType.startsWith("min") || baseType.startsWith("m")

        when (complexity) {
            ChordComplexity.SIMPLE -> {
                // Root + 3rd + 5th
                val third = if (isMinor) root + 3 else root + 4
                val fifth = if (baseType.contains("dim") || baseType.contains("b5")) root + 6 else root + 7
                list.add(third)
                list.add(fifth)
            }

            ChordComplexity.EXTENDED -> {
                // Triad + 7th or 9th
                val third = if (isMinor) root + 3 else root + 4
                val fifth = if (baseType.contains("dim") || baseType.contains("b5")) root + 6 else root + 7
                list.add(third)
                list.add(fifth)

                val seventh = when {
                    baseType.contains("maj7") || baseType.contains("maj9") -> root + 11
                    baseType.contains("dim7") -> root + 9
                    else -> root + 10 // Dominant 7th or minor 7th
                }
                list.add(seventh)

                // Optional 9th if type specifies
                if (baseType.contains("9")) {
                    list.add(root + 14) // 9th is 2 semitones above octave
                }
            }

            ChordComplexity.ADVANCED -> {
                // Jazz / Neo-Soul voicing with extensions & voice leading
                if (baseType.contains("sus4")) {
                    list.add(root + 5) // Perfect 4th
                    list.add(root + 7)
                    list.add(root + 10)
                } else if (baseType.contains("sus2")) {
                    list.add(root + 2) // Major 2nd
                    list.add(root + 7)
                    list.add(root + 11)
                } else {
                    val third = if (isMinor) root + 3 else root + 4
                    val fifth = if (baseType.contains("b5") || baseType.contains("alt")) root + 6 else root + 7
                    val seventh = if (baseType.contains("maj")) root + 11 else root + 10
                    val ninth = if (baseType.contains("alt")) root + 13 else root + 14 // b9 or natural 9th
                    val eleventh = root + 17 // 11th / 4th on top

                    list.add(third)
                    list.add(fifth)
                    list.add(seventh)
                    list.add(ninth)
                    if (baseType.contains("11") || isMinor) {
                        list.add(eleventh)
                    }
                }
            }
        }

        return list.distinct()
    }

    private fun getChordDisplayName(
        root: Int,
        baseType: String,
        complexity: ChordComplexity
    ): String {
        val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val rootName = noteNames[root % 12]

        val suffix = when (complexity) {
            ChordComplexity.SIMPLE -> {
                if (baseType.startsWith("min") || baseType.startsWith("m")) "m" else ""
            }
            ChordComplexity.EXTENDED -> {
                when {
                    baseType.contains("maj9") -> "maj9"
                    baseType.contains("m9") -> "m9"
                    baseType.contains("maj7") -> "maj7"
                    baseType.contains("m7") -> "m7"
                    baseType.contains("7") -> "7"
                    baseType.startsWith("min") -> "m7"
                    else -> "add9"
                }
            }
            ChordComplexity.ADVANCED -> {
                when {
                    baseType.contains("sus4") -> "7sus4"
                    baseType.contains("sus2") -> "sus2"
                    baseType.contains("m7b5") -> "m7(b5)"
                    baseType.contains("7alt") -> "7(alt)"
                    baseType.startsWith("min") -> "m11"
                    else -> "maj9(#11)"
                }
            }
        }

        return "$rootName$suffix"
    }
}

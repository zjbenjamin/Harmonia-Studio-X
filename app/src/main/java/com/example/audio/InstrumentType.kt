package com.example.audio

import androidx.compose.ui.graphics.Color

enum class InstrumentCategory(val displayName: String) {
    KEYBOARDS("Keyboards & Pianos"),
    STRINGS("Strings & Guitars"),
    BRASS_WINDS("Brass & Woodwinds"),
    BASS("Bass & Low End"),
    WORLD("World & Traditional"),
    DRUMS("Drums & Percussion")
}

enum class InstrumentType(
    val id: String,
    val displayName: String,
    val category: InstrumentCategory,
    val description: String,
    val defaultColor: Color,
    val isPercussion: Boolean = false
) {
    // Keyboards
    GRAND_PIANO(
        id = "grand_piano",
        displayName = "Concert Grand Piano",
        category = InstrumentCategory.KEYBOARDS,
        description = "Acoustic grand with rich acoustic harmonics & natural damper decay",
        defaultColor = Color(0xFF38BDF8)
    ),
    ELECTRIC_PIANO(
        id = "electric_piano",
        displayName = "Rhodes E-Piano",
        category = InstrumentCategory.KEYBOARDS,
        description = "Warm electric chime piano with gentle FM bell overtones",
        defaultColor = Color(0xFF0EA5E9)
    ),
    CHURCH_ORGAN(
        id = "church_organ",
        displayName = "Pipe & Drawbar Organ",
        category = InstrumentCategory.KEYBOARDS,
        description = "Sustained harmonic pipe drawbars with rotary chorus vibration",
        defaultColor = Color(0xFF6366F1)
    ),
    SYNTH_POLY_KEYS(
        id = "synth_poly_keys",
        displayName = "Analog Synth Lead",
        category = InstrumentCategory.KEYBOARDS,
        description = "Dual oscillator saw/square polysynth with resonant low-pass filter",
        defaultColor = Color(0xFFA855F7)
    ),

    // Strings
    ACOUSTIC_GUITAR(
        id = "acoustic_guitar",
        displayName = "Nylon Acoustic Guitar",
        category = InstrumentCategory.STRINGS,
        description = "Warm plucked string physics with harmonic overtone decay",
        defaultColor = Color(0xFFF59E0B)
    ),
    ORCHESTRAL_STRINGS(
        id = "orchestral_strings",
        displayName = "Symphonic Strings",
        category = InstrumentCategory.STRINGS,
        description = "Lush symphonic string ensemble pad with soft attack & stereo width",
        defaultColor = Color(0xFFEC4899)
    ),
    CELLO(
        id = "cello",
        displayName = "Solo Cello",
        category = InstrumentCategory.STRINGS,
        description = "Deep expressive bowed solo string instrument with rich vibrato",
        defaultColor = Color(0xFFD946EF)
    ),
    CELTIC_HARP(
        id = "celtic_harp",
        displayName = "Concert Harp",
        category = InstrumentCategory.STRINGS,
        description = "Crystal clear plucked concert harp with delicate resonance",
        defaultColor = Color(0xFFF43F5E)
    ),

    // Brass & Winds
    BRASS_SECTION(
        id = "brass_section",
        displayName = "Epic Brass Section",
        category = InstrumentCategory.BRASS_WINDS,
        description = "Punchy brass horns with bright harmonic flare and high impact",
        defaultColor = Color(0xFFEAB308)
    ),
    TRUMPET(
        id = "trumpet",
        displayName = "Solo Trumpet",
        category = InstrumentCategory.BRASS_WINDS,
        description = "Crisp, soaring trumpet fanfare with natural brass bite",
        defaultColor = Color(0xFFF97316)
    ),
    SAXOPHONE(
        id = "saxophone",
        displayName = "Tenor Saxophone",
        category = InstrumentCategory.BRASS_WINDS,
        description = "Warm jazz saxophone timbre with expressive reedy harmonics",
        defaultColor = Color(0xFFFB923C)
    ),
    FLUTE(
        id = "flute",
        displayName = "Concert Flute",
        category = InstrumentCategory.BRASS_WINDS,
        description = "Silky breathy woodwind flute with gentle flutter vibrato",
        defaultColor = Color(0xFF14B8A6)
    ),

    // Bass
    SLAP_BASS(
        id = "slap_bass",
        displayName = "Electric Slap Bass",
        category = InstrumentCategory.BASS,
        description = "Funky electric bass with punchy percussive transient & tight low-end",
        defaultColor = Color(0xFF10B981)
    ),
    SUB_808_BASS(
        id = "sub_808_bass",
        displayName = "808 Sub Boom Bass",
        category = InstrumentCategory.BASS,
        description = "Deep saturated sub-bass drop with pure low fundamental weight",
        defaultColor = Color(0xFF059669)
    ),
    ACID_303_BASS(
        id = "acid_303_bass",
        displayName = "Acid 303 Bassline",
        category = InstrumentCategory.BASS,
        description = "Resonant squelchy sawtooth synth bass with dynamic sweep",
        defaultColor = Color(0xFF84CC16)
    ),

    // World
    CHINESE_GUZHENG(
        id = "chinese_guzheng",
        displayName = "Chinese Guzheng (古筝)",
        category = InstrumentCategory.WORLD,
        description = "Traditional Chinese 21-string plucked zither with pitch bend ornament",
        defaultColor = Color(0xFFEF4444)
    ),
    ERHU(
        id = "erhu",
        displayName = "Chinese Erhu (二胡)",
        category = InstrumentCategory.WORLD,
        description = "Expressive two-string oriental fiddle with delicate sliding vibrato",
        defaultColor = Color(0xFFDC2626)
    ),
    CHINESE_PIPA(
        id = "chinese_pipa",
        displayName = "Chinese Pipa (琵琶)",
        category = InstrumentCategory.WORLD,
        description = "Four-string plucked lute with rapid tremolo roll and bright wooden percussive bite",
        defaultColor = Color(0xFFF97316)
    ),
    CHINESE_DIZI(
        id = "chinese_dizi",
        displayName = "Chinese Bamboo Flute (竹笛)",
        category = InstrumentCategory.WORLD,
        description = "Traditional bamboo transverse flute with buzzing dimo resonance membrane",
        defaultColor = Color(0xFF10B981)
    ),
    CHINESE_YANGQIN(
        id = "chinese_yangqin",
        displayName = "Chinese Yangqin (扬琴)",
        category = InstrumentCategory.WORLD,
        description = "Hammered dulcimer with bamboo strikers creating shimmering metallic resonance",
        defaultColor = Color(0xFFF59E0B)
    ),
    CHINESE_SUONA(
        id = "chinese_suona",
        displayName = "Chinese Suona (唢呐)",
        category = InstrumentCategory.WORLD,
        description = "Powerful double-reed brass horn with piercing festive harmonics and soaring timbre",
        defaultColor = Color(0xFFEA580C)
    ),
    CHINESE_GUQIN(
        id = "chinese_guqin",
        displayName = "Chinese Guqin (古琴)",
        category = InstrumentCategory.WORLD,
        description = "Ancient seven-string meditative zither with deep silk bass resonance and sliding harmonics",
        defaultColor = Color(0xFF92400E)
    ),
    CHINESE_BIANZHONG(
        id = "chinese_bianzhong",
        displayName = "Chinese Bianzhong Chimes (编钟)",
        category = InstrumentCategory.WORLD,
        description = "Imperial bronze struck chimes with crystalline two-tone overtones and long decay",
        defaultColor = Color(0xFFD97706),
        isPercussion = true
    ),
    MARIMBA(
        id = "marimba",
        displayName = "Mallet Marimba",
        category = InstrumentCategory.WORLD,
        description = "Resonant rosewood wooden mallet bar tone with acoustic hollow attack",
        defaultColor = Color(0xFFF59E0B)
    ),

    // Drums
    DRUM_KIT(
        id = "drum_kit",
        displayName = "Studio Drum Machine",
        category = InstrumentCategory.DRUMS,
        description = "Complete kit with Kick, Snare, Hi-Hats, Toms, Claps, and 808 Percussion",
        defaultColor = Color(0xFF06B6D4),
        isPercussion = true
    );

    companion object {
        fun fromId(id: String): InstrumentType {
            return entries.firstOrNull { it.id == id } ?: GRAND_PIANO
        }
    }
}

package com.example.audio

data class InstrumentPatch(
    val instrumentType: InstrumentType,
    val attackMs: Float = 10f,
    val decayMs: Float = 200f,
    val sustainLevel: Float = 0.7f,
    val releaseMs: Float = 300f,
    val filterCutoff: Float = 8000f,
    val resonance: Float = 0.2f,
    val reverbSend: Float = 0.25f,
    val chorusAmount: Float = 0.0f,
    val drive: Float = 0.0f,
    val octaveTranspose: Int = 0,
    val volume: Float = 0.9f
) {
    companion object {
        fun defaultFor(type: InstrumentType): InstrumentPatch {
            return when (type) {
                InstrumentType.GRAND_PIANO -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 5f,
                    decayMs = 800f,
                    sustainLevel = 0.4f,
                    releaseMs = 400f,
                    filterCutoff = 10000f,
                    reverbSend = 0.3f
                )
                InstrumentType.ELECTRIC_PIANO -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 8f,
                    decayMs = 600f,
                    sustainLevel = 0.5f,
                    releaseMs = 350f,
                    chorusAmount = 0.35f,
                    reverbSend = 0.35f
                )
                InstrumentType.CHURCH_ORGAN -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 25f,
                    decayMs = 100f,
                    sustainLevel = 1.0f,
                    releaseMs = 200f,
                    chorusAmount = 0.4f,
                    reverbSend = 0.5f
                )
                InstrumentType.SYNTH_POLY_KEYS -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 15f,
                    decayMs = 300f,
                    sustainLevel = 0.8f,
                    releaseMs = 300f,
                    filterCutoff = 3500f,
                    resonance = 0.5f,
                    chorusAmount = 0.3f
                )
                InstrumentType.ACOUSTIC_GUITAR -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 4f,
                    decayMs = 500f,
                    sustainLevel = 0.25f,
                    releaseMs = 250f,
                    filterCutoff = 7000f,
                    reverbSend = 0.2f
                )
                InstrumentType.ORCHESTRAL_STRINGS -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 120f,
                    decayMs = 300f,
                    sustainLevel = 0.95f,
                    releaseMs = 600f,
                    chorusAmount = 0.45f,
                    reverbSend = 0.55f
                )
                InstrumentType.CELLO -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 60f,
                    decayMs = 200f,
                    sustainLevel = 0.9f,
                    releaseMs = 400f,
                    octaveTranspose = -1,
                    reverbSend = 0.35f
                )
                InstrumentType.CELTIC_HARP -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 3f,
                    decayMs = 700f,
                    sustainLevel = 0.3f,
                    releaseMs = 500f,
                    reverbSend = 0.45f
                )
                InstrumentType.BRASS_SECTION -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 40f,
                    decayMs = 250f,
                    sustainLevel = 0.85f,
                    releaseMs = 200f,
                    filterCutoff = 5000f,
                    drive = 0.15f
                )
                InstrumentType.TRUMPET -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 20f,
                    decayMs = 150f,
                    sustainLevel = 0.9f,
                    releaseMs = 150f,
                    filterCutoff = 6500f
                )
                InstrumentType.SAXOPHONE -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 30f,
                    decayMs = 200f,
                    sustainLevel = 0.88f,
                    releaseMs = 220f,
                    chorusAmount = 0.2f
                )
                InstrumentType.FLUTE -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 45f,
                    decayMs = 100f,
                    sustainLevel = 0.92f,
                    releaseMs = 180f,
                    reverbSend = 0.4f
                )
                InstrumentType.SLAP_BASS -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 2f,
                    decayMs = 300f,
                    sustainLevel = 0.35f,
                    releaseMs = 120f,
                    octaveTranspose = -1,
                    drive = 0.2f
                )
                InstrumentType.SUB_808_BASS -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 5f,
                    decayMs = 600f,
                    sustainLevel = 0.6f,
                    releaseMs = 300f,
                    octaveTranspose = -2,
                    filterCutoff = 400f,
                    drive = 0.25f
                )
                InstrumentType.ACID_303_BASS -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 8f,
                    decayMs = 220f,
                    sustainLevel = 0.3f,
                    releaseMs = 100f,
                    octaveTranspose = -1,
                    filterCutoff = 2200f,
                    resonance = 0.7f,
                    drive = 0.3f
                )
                InstrumentType.CHINESE_GUZHENG -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 4f,
                    decayMs = 650f,
                    sustainLevel = 0.3f,
                    releaseMs = 450f,
                    reverbSend = 0.4f
                )
                InstrumentType.ERHU -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 70f,
                    decayMs = 200f,
                    sustainLevel = 0.9f,
                    releaseMs = 300f,
                    chorusAmount = 0.25f,
                    reverbSend = 0.35f
                )
                InstrumentType.CHINESE_PIPA -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 3f,
                    decayMs = 450f,
                    sustainLevel = 0.2f,
                    releaseMs = 280f,
                    filterCutoff = 8500f,
                    reverbSend = 0.35f
                )
                InstrumentType.CHINESE_DIZI -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 35f,
                    decayMs = 150f,
                    sustainLevel = 0.92f,
                    releaseMs = 220f,
                    filterCutoff = 7200f,
                    reverbSend = 0.45f
                )
                InstrumentType.CHINESE_YANGQIN -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 2f,
                    decayMs = 600f,
                    sustainLevel = 0.25f,
                    releaseMs = 400f,
                    filterCutoff = 9000f,
                    reverbSend = 0.4f
                )
                InstrumentType.CHINESE_SUONA -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 20f,
                    decayMs = 120f,
                    sustainLevel = 0.95f,
                    releaseMs = 180f,
                    filterCutoff = 9500f,
                    drive = 0.1f
                )
                InstrumentType.CHINESE_GUQIN -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 6f,
                    decayMs = 900f,
                    sustainLevel = 0.3f,
                    releaseMs = 600f,
                    octaveTranspose = -1,
                    filterCutoff = 4500f,
                    reverbSend = 0.5f
                )
                InstrumentType.CHINESE_BIANZHONG -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 2f,
                    decayMs = 1200f,
                    sustainLevel = 0.1f,
                    releaseMs = 1000f,
                    filterCutoff = 11000f,
                    reverbSend = 0.6f
                )
                InstrumentType.MARIMBA -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 2f,
                    decayMs = 350f,
                    sustainLevel = 0.15f,
                    releaseMs = 180f,
                    reverbSend = 0.25f
                )
                InstrumentType.DRUM_KIT -> InstrumentPatch(
                    instrumentType = type,
                    attackMs = 1f,
                    decayMs = 150f,
                    sustainLevel = 0.0f,
                    releaseMs = 80f,
                    drive = 0.1f
                )
            }
        }
    }
}

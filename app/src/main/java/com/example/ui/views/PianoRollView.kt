package com.example.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.InstrumentType
import com.example.data.StudioRepository
import com.example.midi.MidiNote
import com.example.ui.theme.*
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel

@Composable
fun PianoRollView(
    state: StudioUiState,
    viewModel: StudioViewModel,
    onOpenChordGenerator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeClip = state.clips.firstOrNull { it.id == state.selectedClipId }
    val activeTrack = state.tracks.firstOrNull { it.id == (activeClip?.trackId ?: state.selectedTrackId) }
    val notes = remember(activeClip?.notesJson) {
        if (activeClip != null) {
            StudioRepository(com.example.data.StudioDatabase.getInstance(viewModel.getApplication()).studioDao())
                .deserializeNotes(activeClip.notesJson)
        } else emptyList()
    }

    val horizontalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
    ) {
        // Top Toolbar
        Surface(
            color = StudioSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Piano, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${activeTrack?.name ?: "Track"} • ${activeClip?.name ?: "No Clip"}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onOpenChordGenerator,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StudioAmber.copy(alpha = 0.25f),
                            contentColor = StudioAmber
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("piano_roll_chords_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Smart Chords", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Timeline Beat Header (Beats 1..16)
        Surface(
            color = StudioSurfaceElevated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Key spacer
                Spacer(modifier = Modifier.width(50.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    val beatWidth = 32.dp
                    for (beat in 0 until 16) {
                        Box(
                            modifier = Modifier
                                .width(beatWidth)
                                .border(0.5.dp, StudioBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${beat + 1}",
                                fontSize = 9.sp,
                                color = if (beat % 4 == 0) StudioCyan else TextMuted,
                                fontWeight = if (beat % 4 == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Vertical Pitch Grid: notes from C6 (84) down to C3 (48)
        val pitches = (84 downTo 48).toList()

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(pitches) { pitch ->
                val isBlackKey = isPitchBlackKey(pitch)
                val noteName = getPitchName(pitch)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .border(0.5.dp, StudioGridLine),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vertical Piano Key
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .fillMaxHeight()
                            .background(if (isBlackKey) StudioDarkBg else Color(0xFFE2E8F0))
                            .border(0.5.dp, StudioBorder)
                            .clickable {
                                viewModel.onLiveNoteDown(pitch)
                                viewModel.onLiveNoteUp(pitch)
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = noteName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBlackKey) Color(0xFF94A3B8) else Color(0xFF0F172A),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // 16-Beat Pitch Row
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .horizontalScroll(horizontalScrollState)
                            .background(if (isBlackKey) StudioSurface else StudioSurfaceElevated)
                    ) {
                        val beatWidth = 32.dp

                        // Cell click to add note
                        Row(modifier = Modifier.fillMaxHeight()) {
                            for (b in 0 until 16) {
                                Box(
                                    modifier = Modifier
                                        .width(beatWidth)
                                        .fillMaxHeight()
                                        .border(0.5.dp, StudioGridLine)
                                        .clickable {
                                            viewModel.addNoteToActiveClip(pitch, b.toFloat(), 1.0f)
                                        }
                                )
                            }
                        }

                        // Render existing notes on this pitch
                        val notesOnPitch = notes.filter { it.pitch == pitch }
                        for (n in notesOnPitch) {
                            val noteOffset = beatWidth * n.startBeat
                            val noteWidth = beatWidth * n.durationBeats

                            Surface(
                                color = StudioCyan,
                                shape = RoundedCornerShape(3.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                                modifier = Modifier
                                    .offset(x = noteOffset)
                                    .width(noteWidth.coerceAtLeast(16.dp))
                                    .fillMaxHeight(0.9f)
                                    .padding(vertical = 1.dp)
                                    .clickable {
                                        viewModel.deleteNoteFromActiveClip(n.id)
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = noteName,
                                        fontSize = 8.sp,
                                        color = StudioDarkBg,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Playhead indicator
                        val playheadOffset = beatWidth * state.currentBeat
                        Box(
                            modifier = Modifier
                                .offset(x = playheadOffset)
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(StudioAmber)
                        )
                    }
                }
            }
        }
    }
}

private fun isPitchBlackKey(pitch: Int): Boolean {
    val semitone = pitch % 12
    return semitone == 1 || semitone == 3 || semitone == 6 || semitone == 8 || semitone == 10
}

private fun getPitchName(pitch: Int): String {
    val names = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val octave = (pitch / 12) - 1
    return "${names[pitch % 12]}$octave"
}

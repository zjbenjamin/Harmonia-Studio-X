package com.example.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
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
import com.example.audio.InstrumentCategory
import com.example.audio.InstrumentType
import com.example.data.MidiClipEntity
import com.example.data.TrackEntity
import com.example.ui.theme.*
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewTab
import com.example.viewmodel.StudioViewModel

@Composable
fun ArrangerView(
    state: StudioUiState,
    viewModel: StudioViewModel,
    onOpenChordGenerator: () -> Unit,
    onOpenAudioExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = com.example.ui.i18n.StudioI18n.getStrings(state.language)
    var showAddTrackDialog by remember { mutableStateOf(false) }
    val horizontalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
    ) {
        // Quick Action Bar on top of Arranger
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
                // Add Track Button
                Button(
                    onClick = { showAddTrackDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_track_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = StudioDarkBg, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.addTrack, color = StudioDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Smart Chord Generator Trigger
                    FilledTonalButton(
                        onClick = onOpenChordGenerator,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StudioViolet.copy(alpha = 0.25f),
                            contentColor = StudioViolet
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_chord_generator_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.smartChords, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Beat Sequencer Quick Switch
                    FilledTonalButton(
                        onClick = { viewModel.setTab(StudioViewTab.BEAT_SEQUENCER) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StudioEmerald.copy(alpha = 0.25f),
                            contentColor = StudioEmerald
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.drumMachine, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Export Audio Trigger
                    Button(
                        onClick = onOpenAudioExport,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioAmber.copy(alpha = 0.2f),
                            contentColor = StudioAmber
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioAmber.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("arranger_export_audio_button")
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.exportAudio, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Timeline Ruler (Bar 1, 2, 3, 4... 16)
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
                // Header offset spacer
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .padding(start = 12.dp)
                ) {
                    Text("${strings.tracksCount} (${state.tracks.size})", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                }

                // Ruler ticks
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    val totalBars = 8
                    for (b in 1..totalBars) {
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .border(0.5.dp, StudioBorder),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "${strings.bar} $b",
                                color = StudioCyan.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Multi-Track List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            items(state.tracks, key = { it.id }) { track ->
                val isSelected = track.id == state.selectedTrackId
                val trackClips = state.clips.filter { it.trackId == track.id }
                val instrument = InstrumentType.fromId(track.instrumentType)

                TrackArrangerRow(
                    track = track,
                    instrument = instrument,
                    isSelected = isSelected,
                    clips = trackClips,
                    selectedClipId = state.selectedClipId,
                    currentBeat = state.currentBeat,
                    horizontalScrollState = horizontalScrollState,
                    onSelectTrack = { viewModel.selectTrack(track.id) },
                    onSelectClip = { clipId ->
                        viewModel.selectClip(clipId)
                        viewModel.setTab(StudioViewTab.PIANO_ROLL)
                    },
                    onToggleMute = { viewModel.toggleTrackMute(track.id) },
                    onToggleSolo = { viewModel.toggleTrackSolo(track.id) },
                    onVolumeChange = { viewModel.updateTrackVolume(track.id, it) },
                    onDeleteTrack = { viewModel.deleteTrack(track.id) }
                )
            }
        }
    }

    // Add Track Dialog
    if (showAddTrackDialog) {
        var selectedCategory by remember { mutableStateOf(InstrumentCategory.KEYBOARDS) }
        AlertDialog(
            onDismissRequest = { showAddTrackDialog = false },
            title = { Text(strings.addTrackTitle, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Category Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        InstrumentCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioCyan,
                                    selectedLabelColor = StudioDarkBg
                                )
                            )
                        }
                    }

                    // Instruments list in this category
                    val instruments = InstrumentType.entries.filter { it.category == selectedCategory }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        instruments.forEach { inst ->
                            Surface(
                                color = StudioSurfaceElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, inst.defaultColor.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addNewTrack(inst.displayName, inst)
                                        showAddTrackDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(inst.defaultColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(inst.displayName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(inst.description, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                                    }
                                    Icon(Icons.Default.Add, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddTrackDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            },
            containerColor = StudioSurface
        )
    }
}

@Composable
private fun TrackArrangerRow(
    track: TrackEntity,
    instrument: InstrumentType,
    isSelected: Boolean,
    clips: List<MidiClipEntity>,
    selectedClipId: Long?,
    currentBeat: Float,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onSelectTrack: () -> Unit,
    onSelectClip: (Long) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSolo: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onDeleteTrack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(if (isSelected) StudioSurfaceActive else StudioSurface)
            .border(
                1.dp,
                if (isSelected) StudioCyan.copy(alpha = 0.6f) else StudioBorder
            )
            .clickable { onSelectTrack() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Header (Width 130dp)
        Row(
            modifier = Modifier
                .width(130.dp)
                .fillMaxHeight()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(0.7f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(track.colorHex))
            )
            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                // Mute and Solo mini toggles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    // Mute
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (track.isMuted) StudioAmber else StudioSurfaceElevated)
                            .clickable { onToggleMute() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "M",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (track.isMuted) StudioDarkBg else TextSecondary
                        )
                    }
                    // Solo
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (track.isSolo) StudioCyan else StudioSurfaceElevated)
                            .clickable { onToggleSolo() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "S",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (track.isSolo) StudioDarkBg else TextSecondary
                        )
                    }
                }
            }
        }

        // Timeline Clip Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(horizontalScrollState)
                .background(StudioDarkBg)
        ) {
            // Clip blocks
            Row(modifier = Modifier.fillMaxHeight()) {
                val pixelsPerBeat = 24.dp // 96dp per 4-beat bar

                for (clip in clips) {
                    val isClipSelected = clip.id == selectedClipId
                    val clipWidth = pixelsPerBeat * clip.durationBeats

                    Surface(
                        color = Color(track.colorHex).copy(alpha = if (isClipSelected) 0.85f else 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isClipSelected) Color.White else Color(track.colorHex)
                        ),
                        modifier = Modifier
                            .width(clipWidth)
                            .fillMaxHeight(0.85f)
                            .padding(2.dp)
                            .clickable { onSelectClip(clip.id) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = clip.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            // Mini waveform preview lines
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                repeat(12) { idx ->
                                    val barH = ((idx * 7) % 8 + 2).dp
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(barH)
                                            .background(Color.White.copy(alpha = 0.7f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Real-time Playhead Cursor Line
            val playheadOffset = (24.dp * currentBeat)
            Box(
                modifier = Modifier
                    .offset(x = playheadOffset)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(StudioCyan)
            )
        }
    }
}

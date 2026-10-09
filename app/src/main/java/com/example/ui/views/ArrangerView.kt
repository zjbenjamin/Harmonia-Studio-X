package com.example.ui.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
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
    var trackToDelete by remember { mutableStateOf<TrackEntity?>(null) }
    var clipToDelete by remember { mutableStateOf<MidiClipEntity?>(null) }
    val horizontalScrollState = rememberScrollState()

    val totalBars = 8
    val barWidthDp = 96.dp
    val totalTimelineWidth = barWidthDp * totalBars.toFloat()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
    ) {
        // Quick Action Bar on top of Arranger
        Surface(
            color = StudioSurface,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(0.5.dp, StudioBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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

                // Delete Selected Clip Quick Button
                val selectedClip = state.clips.firstOrNull { it.id == state.selectedClipId }
                if (selectedClip != null) {
                    Button(
                        onClick = { clipToDelete = selectedClip },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioRedRecord.copy(alpha = 0.25f),
                            contentColor = StudioRedRecord
                        ),
                        border = BorderStroke(1.dp, StudioRedRecord),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("delete_selected_clip_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = StudioRedRecord, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除音频 [${selectedClip.name.take(6)}]", color = StudioRedRecord, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Delete Selected Track Quick Button
                val selectedTrack = state.tracks.firstOrNull { it.id == state.selectedTrackId }
                if (selectedTrack != null && selectedClip == null) {
                    Button(
                        onClick = { trackToDelete = selectedTrack },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioRedRecord.copy(alpha = 0.18f),
                            contentColor = StudioRedRecord
                        ),
                        border = BorderStroke(1.dp, StudioRedRecord.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("delete_selected_track_btn")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = StudioRedRecord, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除当前音轨", color = StudioRedRecord, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Smart Chord Generator Trigger
                FilledTonalButton(
                    onClick = onOpenChordGenerator,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = StudioViolet.copy(alpha = 0.25f),
                        contentColor = StudioViolet
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
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
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
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
                    border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("arranger_export_audio_button")
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.exportAudio, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Timeline Ruler (Bar 1, 2, 3, 4... 8) strictly aligned with track clips below
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
                // Header offset spacer (strictly 120.dp)
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "${strings.tracksCount} (${state.tracks.size})",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Ruler ticks (synchronized scroll)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    for (b in 1..totalBars) {
                        Box(
                            modifier = Modifier
                                .width(barWidthDp)
                                .height(22.dp)
                                .border(0.5.dp, StudioBorder),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "${strings.bar} $b",
                                color = StudioCyan.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 6.dp)
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
            contentPadding = PaddingValues(vertical = 4.dp)
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
                    timelineWidthDp = totalTimelineWidth,
                    barWidthDp = barWidthDp,
                    totalBars = totalBars,
                    horizontalScrollState = horizontalScrollState,
                    onSelectTrack = { viewModel.selectTrack(track.id) },
                    onSelectClip = { clipId ->
                        viewModel.selectClip(clipId)
                        viewModel.setTab(StudioViewTab.PIANO_ROLL)
                    },
                    onDeleteClip = { clip ->
                        clipToDelete = clip
                    },
                    onToggleMute = { viewModel.toggleTrackMute(track.id) },
                    onToggleSolo = { viewModel.toggleTrackSolo(track.id) },
                    onDeleteTrack = {
                        trackToDelete = track
                    }
                )
            }
        }
    }

    // Confirmation Dialog for Clip Deletion
    if (clipToDelete != null) {
        val target = clipToDelete!!
        AlertDialog(
            onDismissRequest = { clipToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StudioRedRecord) },
            title = { Text("删除音频片段", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    text = "确定要从编曲工作台中删除音频片段【${target.name}】吗？此操作将移除该轨道的音频剪辑数据。",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteClip(target.id)
                        clipToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioRedRecord)
                ) {
                    Text("确认删除", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { clipToDelete = null }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            },
            containerColor = StudioSurface
        )
    }

    // Confirmation Dialog for Track Deletion
    if (trackToDelete != null) {
        val target = trackToDelete!!
        AlertDialog(
            onDismissRequest = { trackToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StudioAmber) },
            title = { Text("删除音轨", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    text = "确定要删除音轨【${target.name}】及其包含的所有音频与 MIDI 片段吗？",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTrack(target.id)
                        trackToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioRedRecord)
                ) {
                    Text("确认删除", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { trackToDelete = null }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            },
            containerColor = StudioSurface
        )
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
                                border = BorderStroke(0.5.dp, StudioBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addNewTrack(inst.displayName, inst)
                                        showAddTrackDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(StudioCyan)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(inst.displayName, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
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
    timelineWidthDp: androidx.compose.ui.unit.Dp,
    barWidthDp: androidx.compose.ui.unit.Dp,
    totalBars: Int,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onSelectTrack: () -> Unit,
    onSelectClip: (Long) -> Unit,
    onDeleteClip: (MidiClipEntity) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSolo: () -> Unit,
    onDeleteTrack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(if (isSelected) StudioSurfaceActive else StudioSurface)
            .border(
                1.dp,
                if (isSelected) StudioCyan.copy(alpha = 0.6f) else StudioBorder
            )
            .clickable { onSelectTrack() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Header (Strictly 120dp wide matching the ruler)
        Row(
            modifier = Modifier
                .width(120.dp)
                .fillMaxHeight()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(0.75f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(track.colorHex))
            )
            Spacer(modifier = Modifier.width(6.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top line: Track name + Direct Delete Track Trash Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = track.name,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Delete Track button
                    IconButton(
                        onClick = onDeleteTrack,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Track",
                            tint = StudioRedRecord.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Bottom line: Mute, Solo mini toggles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute
                    Box(
                        modifier = Modifier
                            .size(20.dp)
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
                            .size(20.dp)
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
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = instrument.displayName.take(4),
                        fontSize = 8.sp,
                        color = TextMuted,
                        maxLines = 1
                    )
                }
            }
        }

        // Timeline Clip Area (Synchronized with Ruler)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(horizontalScrollState)
                .background(StudioDarkBg)
        ) {
            // Background bar separator grid lines
            Row(modifier = Modifier.width(timelineWidthDp).fillMaxHeight()) {
                for (b in 1..totalBars) {
                    Box(
                        modifier = Modifier
                            .width(barWidthDp)
                            .fillMaxHeight()
                            .border(0.25.dp, StudioBorder.copy(alpha = 0.35f))
                    )
                }
            }

            // Accurate Clip blocks positioned by beat offset
            val pixelsPerBeat = 24.dp // 96dp per 4-beat bar

            for (clip in clips) {
                val isClipSelected = clip.id == selectedClipId
                val clipWidth = (pixelsPerBeat * clip.durationBeats).coerceAtLeast(44.dp)
                val clipOffset = (pixelsPerBeat * clip.startBeat)

                Surface(
                    color = Color(track.colorHex).copy(alpha = if (isClipSelected) 0.9f else 0.55f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        if (isClipSelected) 1.5.dp else 1.dp,
                        if (isClipSelected) Color.White else Color(track.colorHex)
                    ),
                    modifier = Modifier
                        .offset(x = clipOffset)
                        .width(clipWidth)
                        .fillMaxHeight(0.85f)
                        .align(Alignment.CenterStart)
                        .clickable { onSelectClip(clip.id) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Clip Header: Name + Direct Delete Icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = clip.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            // Direct Delete Audio Clip Button!
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .clickable { onDeleteClip(clip) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete Clip",
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }

                        // Mini waveform / MIDI preview visual bars
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            repeat(10) { idx ->
                                val barH = ((idx * 7) % 7 + 2).dp
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

            // Real-time Playhead Cursor Line
            val playheadOffset = (pixelsPerBeat * currentBeat)
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

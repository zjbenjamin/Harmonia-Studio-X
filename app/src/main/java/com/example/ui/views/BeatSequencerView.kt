package com.example.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.InstrumentType
import com.example.midi.BeatPattern
import com.example.midi.BeatSequencerDefaults
import com.example.midi.DrumLane
import com.example.midi.DrumStep
import com.example.ui.i18n.StudioI18n
import com.example.ui.theme.*
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import kotlin.math.floor

enum class SequencerFilterTab {
    SYNTH_TRACKS,
    DRUM_KIT,
    ALL_TRACKS
}

@Composable
fun BeatSequencerView(
    state: StudioUiState,
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val strings = StudioI18n.getStrings(state.language)
    val pattern = state.currentBeatPattern

    var filterTab by remember { mutableStateOf(SequencerFilterTab.SYNTH_TRACKS) }
    var selectedVelocityLane by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var pitchDialogLane by remember { mutableStateOf<DrumLane?>(null) }
    var showAddSynthDialog by remember { mutableStateOf(false) }
    var showPresetMenu by remember { mutableStateOf(false) }

    // Synchronized horizontal scroll state for ruler and grid lanes
    val gridScrollState = rememberScrollState()

    // Filtered lanes based on active tab
    val displayedLanes = remember(pattern.lanes, filterTab) {
        when (filterTab) {
            SequencerFilterTab.SYNTH_TRACKS -> pattern.lanes.filter { it.isSynth }
            SequencerFilterTab.DRUM_KIT -> pattern.lanes.filter { !it.isSynth }
            SequencerFilterTab.ALL_TRACKS -> pattern.lanes
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- 1. Top Mini-Transport & Filter Tabs Bar ---
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Sequencer Title, Transport Controls, Presets, Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Play/Pause button and step playhead counter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { viewModel.togglePlay() },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (state.isPlaying) StudioAmber else StudioCyan,
                                contentColor = StudioDarkBg
                            ),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("sequencer_play_button")
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Playhead Visual Step Counter
                        val step = state.currentStepIndex + 1
                        val bar = floor(state.currentBeat / 4f).toInt() + 1
                        val beatInBar = (floor(state.currentBeat).toInt() % 4) + 1
                        Surface(
                            color = StudioDarkBg,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (state.isPlaying) StudioCyan else StudioBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (state.isPlaying) StudioCyan else TextMuted)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = String.format("STEP %02d/16 • BAR %02d.%d", step, bar, beatInBar),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isPlaying) StudioCyan else TextSecondary
                                )
                            }
                        }
                    }

                    // Right: Actions (Presets, + Add Synth, Clear)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Presets Button
                        OutlinedButton(
                            onClick = { showPresetMenu = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioAmber),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioAmber.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("preset_beats_button")
                        ) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.presets, fontSize = 11.sp)
                        }

                        // Add Synth Track
                        FilledTonalButton(
                            onClick = { showAddSynthDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = StudioViolet.copy(alpha = 0.25f),
                                contentColor = StudioViolet
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("add_synth_lane_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(strings.addSynthTrack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Clear Button
                        IconButton(
                            onClick = { viewModel.clearSequencer() },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("clear_sequencer_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = strings.clear, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Row 2: Category Filter Tabs & Swing Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Filter Mode Tabs: Synth Tracks / Drum Kit / All Tracks
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            SequencerFilterTab.SYNTH_TRACKS to strings.tabSynthTracks,
                            SequencerFilterTab.DRUM_KIT to strings.tabDrumTracks,
                            SequencerFilterTab.ALL_TRACKS to strings.tabAllTracks
                        ).forEach { (tab, label) ->
                            val isSelected = filterTab == tab
                            val count = when (tab) {
                                SequencerFilterTab.SYNTH_TRACKS -> pattern.lanes.count { it.isSynth }
                                SequencerFilterTab.DRUM_KIT -> pattern.lanes.count { !it.isSynth }
                                SequencerFilterTab.ALL_TRACKS -> pattern.lanes.size
                            }
                            Surface(
                                color = if (isSelected) StudioCyan.copy(alpha = 0.2f) else StudioSurfaceElevated,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) StudioCyan else StudioBorder
                                ),
                                modifier = Modifier
                                    .clickable { filterTab = tab }
                                    .testTag("filter_tab_${tab.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (tab) {
                                            SequencerFilterTab.SYNTH_TRACKS -> Icons.Default.GraphicEq
                                            SequencerFilterTab.DRUM_KIT -> Icons.Default.RadioButtonChecked
                                            SequencerFilterTab.ALL_TRACKS -> Icons.Default.GridView
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) StudioCyan else TextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$label ($count)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) StudioCyan else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Swing Control Slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(170.dp)
                    ) {
                        Text(
                            text = "Swing: ${pattern.swingPercent}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = StudioViolet
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Slider(
                            value = pattern.swingPercent.toFloat(),
                            onValueChange = { viewModel.setSequencerSwing(it.toInt()) },
                            valueRange = 0f..75f,
                            steps = 14,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = StudioViolet,
                                activeTrackColor = StudioViolet,
                                inactiveTrackColor = StudioBorder
                            )
                        )
                    }
                }
            }
        }

        // --- 2. Responsive 16-Step Grid Sequencer with Playhead Visual Indicator ---
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val totalWidth = maxWidth
            val headerWidth = 140.dp
            // Calculate step width dynamically: either fill available width or use comfortable 36dp min width
            val availableGridWidth = totalWidth - headerWidth
            val stepWidth = (availableGridWidth / 16f).coerceAtLeast(36.dp)

            Surface(
                color = StudioSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    // Header Ruler Row with Step Numbers & Luminous Playhead Indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Track header column spacer
                        Box(
                            modifier = Modifier
                                .width(headerWidth)
                                .padding(end = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "TRACK / PITCH",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // 16 Step Header Buttons with Synchronized Scroll
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(gridScrollState),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (stepIdx in 0 until pattern.stepCount) {
                                val isCurrentStep = state.isPlaying && state.currentStepIndex == stepIdx
                                val isBeatStart = stepIdx % 4 == 0
                                val beatNumber = (stepIdx / 4) + 1

                                Column(
                                    modifier = Modifier.width(stepWidth),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Playhead indicator pointer / chevron
                                    Box(
                                        modifier = Modifier.height(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCurrentStep) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Playhead",
                                                tint = StudioCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Step pill
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(20.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when {
                                                    isCurrentStep -> StudioCyan
                                                    isBeatStart -> StudioSurfaceActive
                                                    else -> StudioSurfaceElevated
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                if (isCurrentStep) Color.White else StudioBorder.copy(alpha = 0.5f),
                                                RoundedCornerShape(4.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${stepIdx + 1}",
                                            fontSize = 9.sp,
                                            fontWeight = if (isCurrentStep || isBeatStart) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isCurrentStep -> StudioDarkBg
                                                isBeatStart -> TextPrimary
                                                else -> TextMuted
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = StudioBorder, thickness = 1.dp)

                    // Track Rows List
                    if (displayedLanes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.MusicOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No tracks in this category", color = TextSecondary, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showAddSynthDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioViolet)
                                ) {
                                    Text(strings.addSynthTrack, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(displayedLanes, key = { it.id }) { lane ->
                                SynthSequencerRow(
                                    lane = lane,
                                    headerWidth = headerWidth,
                                    stepWidth = stepWidth,
                                    currentStepIndex = state.currentStepIndex,
                                    isPlaying = state.isPlaying,
                                    scrollState = gridScrollState,
                                    onAudition = { viewModel.auditionLane(lane.id) },
                                    onToggleMute = { viewModel.toggleLaneMute(lane.id) },
                                    onToggleSolo = { viewModel.toggleLaneSolo(lane.id) },
                                    onOpenPitchSelect = { pitchDialogLane = lane },
                                    onToggleStep = { stepIdx -> viewModel.toggleStep(lane.id, stepIdx) },
                                    onOpenVelocity = { stepIdx -> selectedVelocityLane = lane.id to stepIdx }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. Bottom Action Bar: Commit / Apply to Arranger Timeline ---
        Button(
            onClick = { viewModel.insertBeatPatternToTimeline() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("apply_sequencer_pattern_button"),
            colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = StudioDarkBg)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.applyBeatsToTimeline,
                color = StudioDarkBg,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }

    // --- Dialog 1: Step Velocity & Ratchet Editor ---
    if (selectedVelocityLane != null) {
        val (laneId, stepIdx) = selectedVelocityLane!!
        val lane = pattern.lanes.firstOrNull { it.id == laneId }
        val step = lane?.steps?.getOrNull(stepIdx)
        if (lane != null && step != null) {
            AlertDialog(
                onDismissRequest = { selectedVelocityLane = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(lane.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${lane.name} • Step ${stepIdx + 1}",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.stepVelocityTitle, fontSize = 12.sp, color = TextSecondary)
                            Text("${(step.velocity * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StudioViolet)
                        }
                        Slider(
                            value = step.velocity,
                            onValueChange = { viewModel.setStepVelocity(laneId, stepIdx, it) },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = StudioViolet,
                                activeTrackColor = StudioViolet,
                                inactiveTrackColor = StudioBorder
                            )
                        )

                        // Quick presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(0.3f to "Ghost (30%)", 0.7f to "Normal (70%)", 1.0f to "Accent (100%)").forEach { (v, label) ->
                                OutlinedButton(
                                    onClick = { viewModel.setStepVelocity(laneId, stepIdx, v) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(label, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { selectedVelocityLane = null },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan)
                    ) {
                        Text(strings.done, color = StudioDarkBg, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = StudioSurface
            )
        }
    }

    // --- Dialog 2: Synth Pitch & Octave Selector ---
    if (pitchDialogLane != null) {
        val lane = pitchDialogLane!!
        var currentPitch by remember { mutableIntStateOf(lane.midiNote) }
        val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val currentOctave = (currentPitch / 12) - 1
        val currentNoteIdx = (currentPitch % 12 + 12) % 12

        AlertDialog(
            onDismissRequest = { pitchDialogLane = null },
            title = {
                Text(
                    text = "${strings.pitchSelectTitle}: ${lane.name}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Current Note Display
                    Surface(
                        color = StudioDarkBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Current Pitch:", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                text = "${noteNames[currentNoteIdx]}$currentOctave (MIDI $currentPitch)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioCyan
                            )
                        }
                    }

                    // Octave Chooser (1 to 5)
                    Text("Octave:", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..5).forEach { oct ->
                            val isSelected = oct == currentOctave
                            OutlinedButton(
                                onClick = {
                                    val newPitch = (oct + 1) * 12 + currentNoteIdx
                                    currentPitch = newPitch
                                    viewModel.setLanePitch(lane.id, newPitch, "${noteNames[currentNoteIdx]}$oct")
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) StudioViolet else Color.Transparent,
                                    contentColor = if (isSelected) StudioDarkBg else TextPrimary
                                ),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text("Oct $oct", fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    // 12 Semitone Notes Grid
                    Text("Root Note:", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (row in 0..2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (col in 0..3) {
                                    val idx = row * 4 + col
                                    val isSelected = idx == currentNoteIdx
                                    val noteName = noteNames[idx]
                                    val isSharp = noteName.contains("#")

                                    Surface(
                                        color = when {
                                            isSelected -> StudioCyan
                                            isSharp -> StudioSurfaceElevated
                                            else -> StudioSurfaceActive
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) StudioCyan else StudioBorder
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                val newPitch = (currentOctave + 1) * 12 + idx
                                                currentPitch = newPitch
                                                viewModel.setLanePitch(lane.id, newPitch, "$noteName$currentOctave")
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = noteName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) StudioDarkBg else TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { pitchDialogLane = null },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan)
                ) {
                    Text(strings.done, color = StudioDarkBg)
                }
            },
            containerColor = StudioSurface
        )
    }

    // --- Dialog 3: Add New Synth Track Modal ---
    if (showAddSynthDialog) {
        val synthOptions = listOf(
            Triple(InstrumentType.SYNTH_POLY_KEYS, "Cyber Lead Synth", 60),
            Triple(InstrumentType.ACID_303_BASS, "Acid 303 Bass", 36),
            Triple(InstrumentType.SUB_808_BASS, "Sub 808 Bass", 38),
            Triple(InstrumentType.SLAP_BASS, "Electric Slap Bass", 40),
            Triple(InstrumentType.CELTIC_HARP, "Neon Pluck Harp", 67),
            Triple(InstrumentType.ORCHESTRAL_STRINGS, "Cosmic String Pad", 57),
            Triple(InstrumentType.GRAND_PIANO, "Grand Piano Stabs", 60),
            Triple(InstrumentType.ELECTRIC_PIANO, "Rhodes E-Piano", 64),
            Triple(InstrumentType.CHURCH_ORGAN, "Organ Chords", 60),
            Triple(InstrumentType.MARIMBA, "Mallet Marimba", 72)
        )

        AlertDialog(
            onDismissRequest = { showAddSynthDialog = false },
            title = {
                Text(strings.addSynthTrack, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Select a synthesizer instrument to sequence:", fontSize = 12.sp, color = TextSecondary)

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(synthOptions) { (inst, name, pitch) ->
                            Surface(
                                color = StudioSurfaceElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, inst.defaultColor.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addSynthLane(
                                            instrumentType = inst,
                                            pitch = pitch,
                                            name = name,
                                            colorHex = inst.defaultColor.value.toLong()
                                        )
                                        showAddSynthDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(inst.defaultColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = inst.defaultColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                        Text(inst.category.displayName, fontSize = 10.sp, color = TextMuted)
                                    }
                                    Icon(Icons.Default.Add, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddSynthDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            },
            containerColor = StudioSurface
        )
    }

    // --- Preset Grooves Menu ---
    if (showPresetMenu) {
        AlertDialog(
            onDismissRequest = { showPresetMenu = false },
            title = { Text(strings.selectPresetGroove, color = TextPrimary, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BeatSequencerDefaults.getPresetPatterns().forEach { preset ->
                        Surface(
                            color = StudioSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.loadPresetBeat(preset)
                                    showPresetMenu = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(preset.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = "${preset.lanes.count { it.isSynth }} Synths • ${preset.lanes.count { !it.isSynth }} Drums • Swing ${preset.swingPercent}%",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(Icons.Default.Check, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetMenu = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            },
            containerColor = StudioSurface
        )
    }
}

/**
 * A single responsive track row in the Step Sequencer with interactive 16-step pads,
 * instrument timbre icon, pitch badge, mute/solo, and visual firing playback pulses!
 */
@Composable
fun SynthSequencerRow(
    lane: DrumLane,
    headerWidth: androidx.compose.ui.unit.Dp,
    stepWidth: androidx.compose.ui.unit.Dp,
    currentStepIndex: Int,
    isPlaying: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    onAudition: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSolo: () -> Unit,
    onOpenPitchSelect: () -> Unit,
    onToggleStep: (Int) -> Unit,
    onOpenVelocity: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Header (Fixed Width): Instrument Icon, Name, Note Badge, M/S, Audition
        Surface(
            color = if (lane.isSolo) StudioAmber.copy(alpha = 0.15f) else StudioSurfaceElevated,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (lane.isSolo) StudioAmber else lane.color.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .width(headerWidth)
                .fillMaxHeight()
                .padding(end = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Instrument Icon + Audition trigger
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(lane.color.copy(alpha = 0.25f))
                        .clickable { onAudition() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (lane.isSynth) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                        contentDescription = "Audition",
                        tint = lane.color,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Name & Pitch Badge
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = lane.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (lane.isMuted) TextMuted else TextPrimary,
                        maxLines = 1
                    )
                    // If synth track, show clickable pitch badge (e.g. C2, C4)
                    if (lane.isSynth) {
                        Text(
                            text = lane.displayNote,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = lane.color,
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(lane.color.copy(alpha = 0.15f))
                                .clickable { onOpenPitchSelect() }
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }

                // Mute and Solo Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Solo
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (lane.isSolo) StudioAmber else StudioDarkBg)
                            .clickable { onToggleSolo() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "S",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lane.isSolo) StudioDarkBg else TextMuted
                        )
                    }
                    // Mute
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (lane.isMuted) StudioCoral else StudioDarkBg)
                            .clickable { onToggleMute() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "M",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lane.isMuted) StudioDarkBg else TextMuted
                        )
                    }
                }
            }
        }

        // 16 Step Pads Row (Synchronized Horizontal Scroll with Ruler)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (stepIdx in 0 until lane.steps.size) {
                val step = lane.steps[stepIdx]
                val isCurrentPlayhead = isPlaying && currentStepIndex == stepIdx
                val isFiring = isCurrentPlayhead && step.active
                val isBeatGroupStart = stepIdx % 4 == 0

                // Animated firing pulse for active playing steps
                val animatedScale by animateFloatAsState(
                    targetValue = if (isFiring) 1.08f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
                    label = "step_fire_scale"
                )

                // Pad Background Color based on Active, Playhead & Velocity
                val basePadColor = when {
                    step.active -> lane.color.copy(alpha = 0.35f + (step.velocity * 0.55f))
                    isCurrentPlayhead -> StudioCyan.copy(alpha = 0.12f)
                    isBeatGroupStart -> StudioSurfaceActive
                    else -> StudioSurfaceElevated
                }

                Box(
                    modifier = Modifier
                        .width(stepWidth)
                        .fillMaxHeight()
                        .scale(animatedScale)
                        .clip(RoundedCornerShape(5.dp))
                        .background(basePadColor)
                        .border(
                            width = when {
                                isFiring -> 2.dp
                                isCurrentPlayhead -> 1.5.dp
                                step.active -> 1.dp
                                else -> 0.5.dp
                            },
                            color = when {
                                isFiring -> Color.White
                                isCurrentPlayhead -> StudioCyan
                                step.active -> lane.color
                                else -> StudioBorder.copy(alpha = 0.4f)
                            },
                            shape = RoundedCornerShape(5.dp)
                        )
                        .clickable { onToggleStep(stepIdx) }
                        .testTag("step_${lane.id}_$stepIdx"),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner Velocity Indicator Bar at the bottom of active pads
                    if (step.active) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.BottomCenter)
                                .background(Color.White.copy(alpha = 0.8f))
                        )
                    }

                    // Optional Long-press / Click for Velocity Badge
                    if (step.active) {
                        Text(
                            text = if (step.velocity >= 0.95f) "▲" else "${(step.velocity * 10).toInt()}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFiring) Color.White else Color.White.copy(alpha = 0.9f)
                        )
                    } else if (isCurrentPlayhead) {
                        // Playhead ghost dot
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(StudioCyan)
                        )
                    }
                }
            }
        }
    }
}

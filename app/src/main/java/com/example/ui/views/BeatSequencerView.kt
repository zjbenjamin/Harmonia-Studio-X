package com.example.ui.views

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.midi.BeatPattern
import com.example.midi.BeatSequencerDefaults
import com.example.midi.DrumLane
import com.example.ui.theme.*
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel

@Composable
fun BeatSequencerView(
    state: StudioUiState,
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val pattern = state.currentBeatPattern
    var selectedVelocityLane by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var showPresetMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sequencer Header Controls: Presets, Swing Slider, Clear, Apply
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Beat Sequencer",
                            tint = StudioCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pattern.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Preset Button
                        OutlinedButton(
                            onClick = { showPresetMenu = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioAmber),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioAmber.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("preset_beats_button")
                        ) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Presets", fontSize = 11.sp)
                        }

                        // Clear Button
                        IconButton(
                            onClick = { viewModel.clearSequencer() },
                            modifier = Modifier.size(32.dp).testTag("clear_sequencer_button")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Swing & Groove Quantization Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Swing & Groove: ${pattern.swingPercent}%",
                        color = StudioViolet,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(130.dp)
                    )
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

        // 16-Step Beat Grid Display
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Step Number & Active Playhead Ruler
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lane label spacer
                    Spacer(modifier = Modifier.width(90.dp))

                    // 16 step position indicators
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (stepIdx in 0 until pattern.stepCount) {
                            val isCurrentStep = state.isPlaying && state.currentStepIndex == stepIdx
                            val isBeatGroupStart = stepIdx % 4 == 0
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            isCurrentStep -> StudioCyan
                                            isBeatGroupStart -> StudioSurfaceActive
                                            else -> StudioSurfaceElevated
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${stepIdx + 1}",
                                    fontSize = 9.sp,
                                    fontWeight = if (isCurrentStep) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrentStep) StudioDarkBg else TextMuted
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = StudioBorder)

                // Drum Lanes List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(pattern.lanes, key = { it.id }) { lane ->
                        DrumLaneRow(
                            lane = lane,
                            currentStepIndex = state.currentStepIndex,
                            isPlaying = state.isPlaying,
                            onTriggerDrum = { viewModel.onLiveDrumTrigger(lane.midiNote) },
                            onToggleStep = { stepIdx -> viewModel.toggleStep(lane.id, stepIdx) },
                            onSelectStepVelocity = { stepIdx -> selectedVelocityLane = lane.id to stepIdx }
                        )
                    }
                }
            }
        }

        // Bottom Action: Apply to Drum Track
        Button(
            onClick = { viewModel.insertBeatPatternToTimeline() },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("apply_beat_pattern_button"),
            colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = StudioDarkBg)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Apply Drum Pattern to Timeline",
                color = StudioDarkBg,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }

    // Velocity Adjust Sheet / Dialog
    if (selectedVelocityLane != null) {
        val (laneId, stepIdx) = selectedVelocityLane!!
        val lane = pattern.lanes.firstOrNull { it.id == laneId }
        val step = lane?.steps?.getOrNull(stepIdx)
        if (step != null) {
            AlertDialog(
                onDismissRequest = { selectedVelocityLane = null },
                title = { Text("${lane.name} - Step ${stepIdx + 1} Velocity", color = TextPrimary, fontSize = 15.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Velocity Accent: ${(step.velocity * 100).toInt()}%", color = StudioViolet)
                        Slider(
                            value = step.velocity,
                            onValueChange = { viewModel.setStepVelocity(laneId, stepIdx, it) },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = StudioViolet, activeTrackColor = StudioViolet)
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { selectedVelocityLane = null }) {
                        Text("Done")
                    }
                },
                containerColor = StudioSurface
            )
        }
    }

    // Preset Patterns Picker Dialog
    if (showPresetMenu) {
        AlertDialog(
            onDismissRequest = { showPresetMenu = false },
            title = { Text("Select Groove Preset", color = TextPrimary) },
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
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(preset.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Swing: ${preset.swingPercent}%", color = TextSecondary, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPresetMenu = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = StudioSurface
        )
    }
}

@Composable
private fun DrumLaneRow(
    lane: DrumLane,
    currentStepIndex: Int,
    isPlaying: Boolean,
    onTriggerDrum: () -> Unit,
    onToggleStep: (Int) -> Unit,
    onSelectStepVelocity: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drum Pad Trigger Button (tap to test sound)
        Surface(
            color = StudioSurfaceElevated,
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, lane.color.copy(alpha = 0.4f)),
            modifier = Modifier
                .width(90.dp)
                .fillMaxHeight()
                .clickable { onTriggerDrum() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(lane.color)
                )
                Text(
                    text = lane.name,
                    fontSize = 11.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 16 Step Buttons
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            lane.steps.forEachIndexed { idx, step ->
                val isBeatBoundary = idx % 4 == 0
                val isCurrentPlayingStep = isPlaying && currentStepIndex == idx

                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                step.active -> lane.color.copy(alpha = 0.35f + (step.velocity * 0.65f))
                                isBeatBoundary -> StudioSurfaceActive
                                else -> StudioSurfaceElevated
                            }
                        )
                        .border(
                            width = if (isCurrentPlayingStep) 1.5.dp else 1.dp,
                            color = when {
                                isCurrentPlayingStep -> StudioCyan
                                step.active -> lane.color
                                else -> StudioBorder
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onToggleStep(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    if (step.active) {
                        Box(
                            modifier = Modifier
                                .size(if (step.velocity > 0.8f) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.InstrumentCategory
import com.example.audio.InstrumentPatch
import com.example.audio.InstrumentType
import com.example.ui.theme.*
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel

@Composable
fun SoundLibraryView(
    state: StudioUiState,
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(InstrumentCategory.KEYBOARDS) }
    var inspectingInstrument by remember { mutableStateOf(InstrumentType.GRAND_PIANO) }
    var currentPatch by remember { mutableStateOf(viewModel.audioEngine.getPatch(inspectingInstrument)) }

    LaunchedEffect(inspectingInstrument) {
        currentPatch = viewModel.audioEngine.getPatch(inspectingInstrument)
    }

    val activeTrack = state.tracks.firstOrNull { it.id == state.selectedTrackId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Category Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            InstrumentCategory.entries.forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.displayName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudioCyan,
                        selectedLabelColor = StudioDarkBg,
                        containerColor = StudioSurfaceElevated,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        // Split Layout: Instruments List on Top/Left, Synth Rack Knobs on Bottom
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Instruments Grid / List
            val categoryInstruments = InstrumentType.entries.filter { it.category == selectedCategory }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categoryInstruments) { inst ->
                    val isInspecting = inspectingInstrument == inst
                    val isUsedBySelectedTrack = activeTrack != null &&
                            InstrumentType.fromId(activeTrack.instrumentType) == inst

                    Surface(
                        color = if (isInspecting) StudioSurfaceActive else StudioSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isInspecting) inst.defaultColor else StudioBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { inspectingInstrument = inst }
                            .testTag("instrument_card_${inst.id}")
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = inst.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    if (isUsedBySelectedTrack) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(Active Track)",
                                            fontSize = 9.sp,
                                            color = StudioCyan,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Text(
                                    text = inst.description,
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = { viewModel.auditionInstrument(inst) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Audition", tint = StudioAmber, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Synthesizer Parameter Tweaker Rack
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYNTH ENGINE: ${inspectingInstrument.displayName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = inspectingInstrument.defaultColor
                    )

                    // Assign to track button
                    if (activeTrack != null) {
                        OutlinedButton(
                            onClick = {
                                viewModel.assignInstrumentToTrack(activeTrack.id, inspectingInstrument)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyan),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Use on ${activeTrack.name}", fontSize = 10.sp)
                        }
                    }
                }

                // Envelope Sliders (ADSR)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Attack
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Attack: ${currentPatch.attackMs.toInt()}ms", fontSize = 10.sp, color = TextSecondary)
                        Slider(
                            value = currentPatch.attackMs,
                            onValueChange = {
                                currentPatch = currentPatch.copy(attackMs = it)
                                viewModel.updateInstrumentPatch(currentPatch)
                            },
                            valueRange = 1f..300f
                        )
                    }
                    // Release
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Release: ${currentPatch.releaseMs.toInt()}ms", fontSize = 10.sp, color = TextSecondary)
                        Slider(
                            value = currentPatch.releaseMs,
                            onValueChange = {
                                currentPatch = currentPatch.copy(releaseMs = it)
                                viewModel.updateInstrumentPatch(currentPatch)
                            },
                            valueRange = 20f..1500f
                        )
                    }
                }

                // Filter & Reverb
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Filter Cutoff
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Filter Cutoff: ${(currentPatch.filterCutoff / 1000).toInt()}kHz", fontSize = 10.sp, color = TextSecondary)
                        Slider(
                            value = currentPatch.filterCutoff,
                            onValueChange = {
                                currentPatch = currentPatch.copy(filterCutoff = it)
                                viewModel.updateInstrumentPatch(currentPatch)
                            },
                            valueRange = 200f..16000f,
                            colors = SliderDefaults.colors(thumbColor = StudioViolet, activeTrackColor = StudioViolet)
                        )
                    }
                    // Reverb Send
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reverb: ${(currentPatch.reverbSend * 100).toInt()}%", fontSize = 10.sp, color = TextSecondary)
                        Slider(
                            value = currentPatch.reverbSend,
                            onValueChange = {
                                currentPatch = currentPatch.copy(reverbSend = it)
                                viewModel.updateInstrumentPatch(currentPatch)
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = StudioAmber, activeTrackColor = StudioAmber)
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.StudioI18n
import com.example.ui.theme.*
import com.example.viewmodel.StudioUiState
import kotlin.math.floor

@Composable
fun TopTransportBar(
    state: StudioUiState,
    onTogglePlay: () -> Unit,
    onToggleRecord: () -> Unit,
    onStop: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleMetronome: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenCollab: () -> Unit,
    onExportAudio: () -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = StudioI18n.getStrings(state.language)
    var showBpmDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StudioBorder, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
        color = StudioSurface,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Row 1: Project Title, Timecode OLED, and Sync Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Project info & Share Code
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.currentProject?.title ?: "Harmonia Project",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(StudioEmerald)
                        )
                        Text(
                            text = "${state.currentProject?.shareCode ?: "HRMN"} • ${state.collaborators.size} connected",
                            color = StudioCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onOpenCollab() }
                        )
                    }
                }

                // OLED Timecode Display
                val currentBeat = state.currentBeat
                val bar = floor(currentBeat / 4f).toInt() + 1
                val beatInBar = (floor(currentBeat).toInt() % 4) + 1
                val subBeat = ((currentBeat - floor(currentBeat)) * 100).toInt()
                val timeStr = String.format("%02d:%02d:%02d", bar, beatInBar, subBeat)

                Surface(
                    color = StudioDarkBg,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = timeStr,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = StudioCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // BPM & Time Signature Chip
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.clickable { showBpmDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${state.currentProject?.bpm ?: 120} BPM",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StudioAmber
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = state.currentProject?.timeSignature ?: "4/4",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Language Switcher Chip (中文 / EN)
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioViolet.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .clickable { onToggleLanguage() }
                        .padding(start = 6.dp)
                        .testTag("language_toggle_button")
                ) {
                    Text(
                        text = if (state.language == AppLanguage.SIMPLIFIED_CHINESE) "中文" else "EN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioViolet,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Transport Buttons (Loop, Metronome, Play, Stop, Record) and Volume
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Secondary toggles: Loop & Metronome
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Loop Toggle
                    val isLooping = state.currentProject?.isLooping == true
                    FilledTonalIconButton(
                        onClick = onToggleLoop,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isLooping) StudioCyan.copy(alpha = 0.2f) else StudioSurfaceElevated,
                            contentColor = if (isLooping) StudioCyan else TextSecondary
                        ),
                        modifier = Modifier.size(38.dp).testTag("loop_button")
                    ) {
                        Icon(Icons.Default.Repeat, contentDescription = "Loop", modifier = Modifier.size(18.dp))
                    }

                    // Metronome Toggle
                    val isMetro = state.isMetronomeOn
                    FilledTonalIconButton(
                        onClick = onToggleMetronome,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isMetro) StudioAmber.copy(alpha = 0.2f) else StudioSurfaceElevated,
                            contentColor = if (isMetro) StudioAmber else TextSecondary
                        ),
                        modifier = Modifier.size(38.dp).testTag("metronome_button")
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = "Metronome", modifier = Modifier.size(18.dp))
                    }
                }

                // Core Transport Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stop Button
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(38.dp)
                            .background(StudioSurfaceElevated, CircleShape)
                            .border(1.dp, StudioBorder, CircleShape)
                            .testTag("stop_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }

                    // Play / Pause Button
                    val isPlaying = state.isPlaying
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                Brush.linearGradient(
                                    if (isPlaying) listOf(StudioCyan, StudioViolet)
                                    else listOf(StudioCyan.copy(alpha = 0.8f), StudioViolet.copy(alpha = 0.8f))
                                ),
                                CircleShape
                            )
                            .testTag("play_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = StudioDarkBg,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Record Button with pulsing ring when active
                    val isRec = state.isRecording
                    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
                    val recAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "rec_alpha"
                    )

                    IconButton(
                        onClick = onToggleRecord,
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (isRec) StudioRedRecord.copy(alpha = recAlpha) else StudioSurfaceElevated,
                                CircleShape
                            )
                            .border(1.5.dp, if (isRec) StudioRedRecord else StudioBorder, CircleShape)
                            .testTag("record_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isRec) Color.White else StudioRedRecord)
                        )
                    }
                }

                // Master Volume Slider & Export Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(80.dp)
                    ) {
                        Icon(
                            imageVector = if (state.masterVolume > 0.5f) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            contentDescription = "Master Volume",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Slider(
                            value = state.masterVolume,
                            onValueChange = onVolumeChange,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 2.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = StudioCyan,
                                activeTrackColor = StudioCyan,
                                inactiveTrackColor = StudioBorder
                            )
                        )
                    }

                    // Export Audio Button
                    FilledTonalIconButton(
                        onClick = onExportAudio,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = StudioCyan.copy(alpha = 0.2f),
                            contentColor = StudioCyan
                        ),
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("top_bar_export_audio_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export Audio", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    // BPM Adjust Dialog
    if (showBpmDialog) {
        var tempoInput by remember { mutableStateOf((state.currentProject?.bpm ?: 120).toString()) }
        AlertDialog(
            onDismissRequest = { showBpmDialog = false },
            title = { Text(strings.bpmDialogTitle, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${strings.tempoLabel}: $tempoInput", color = TextSecondary)
                    Slider(
                        value = (tempoInput.toIntOrNull() ?: 120).toFloat(),
                        onValueChange = { tempoInput = it.toInt().toString() },
                        valueRange = 40f..240f,
                        steps = 200,
                        colors = SliderDefaults.colors(thumbColor = StudioAmber, activeTrackColor = StudioAmber)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(80, 100, 120, 128, 140, 160).forEach { presetBpm ->
                            OutlinedButton(
                                onClick = { tempoInput = presetBpm.toString() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("$presetBpm", fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bpm = tempoInput.toIntOrNull() ?: 120
                        onBpmChange(bpm)
                        showBpmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan)
                ) {
                    Text(strings.apply, color = StudioDarkBg)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBpmDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            },
            containerColor = StudioSurface
        )
    }
}

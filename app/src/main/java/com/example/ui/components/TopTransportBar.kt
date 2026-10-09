package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.StudioI18n
import com.example.ui.theme.*
import com.example.viewmodel.StudioUiState
import kotlin.math.floor
import kotlin.math.roundToInt

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
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = StudioI18n.getStrings(state.language)
    var showBpmDialog by remember { mutableStateOf(false) }
    var showVolumePopup by remember { mutableStateOf(false) }

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
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // --- Row 1: Brand / Project Identity, User Sync Badge, OLED Clock, Settings ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: App Logo, Project title & User / Cloud Sync Status
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // App Logo Icon
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(StudioCyan, StudioViolet)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Logo",
                            tint = StudioDarkBg,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Text(
                        text = state.currentProject?.title ?: "Harmonia Project",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )

                    // User Auth / Cloud Sync Status Chip
                    val profile = state.userProfile
                    Surface(
                        color = if (profile.isLoggedIn) Color(profile.avatarColorHex).copy(alpha = 0.18f) else StudioSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            0.5.dp,
                            if (profile.isLoggedIn) Color(profile.avatarColorHex).copy(alpha = 0.6f) else StudioBorder
                        ),
                        modifier = Modifier
                            .clickable { onOpenCollab() }
                            .testTag("top_bar_user_profile_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            if (profile.isLoggedIn) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(profile.avatarColorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = profile.provider.badgeText.take(1),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (profile.provider.badgeText == "𝕏") Color.Black else Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = profile.provider.displayName,
                                    color = TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(StudioViolet)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "登录同步",
                                    color = StudioViolet,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Right: OLED Timecode + BPM + Language Toggle + About Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // OLED Timecode Display
                    val currentBeat = state.currentBeat
                    val bar = floor(currentBeat / 4f).toInt() + 1
                    val beatInBar = (floor(currentBeat).toInt() % 4) + 1
                    val subBeat = ((currentBeat - floor(currentBeat)) * 100).toInt()
                    val timeStr = String.format("%02d:%02d:%02d", bar, beatInBar, subBeat)

                    Surface(
                        color = StudioDarkBg,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Text(
                            text = timeStr,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = StudioCyan,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    // BPM Chip
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier
                            .clickable { showBpmDialog = true }
                            .testTag("bpm_setting_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${state.currentProject?.bpm ?: 120}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioAmber
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "BPM",
                                fontSize = 9.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Language Switcher Chip
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, StudioViolet.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clickable { onToggleLanguage() }
                            .testTag("language_toggle_button")
                    ) {
                        Text(
                            text = if (state.language == AppLanguage.SIMPLIFIED_CHINESE) "中" else "EN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioViolet,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    // About App Action Button
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clickable { onOpenAbout() }
                            .testTag("about_app_button")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "关于软件",
                                    tint = StudioCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "关于",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioCyan
                                )
                            }
                        }
                    }
                }
            }

            // --- Row 2: Precision Transport Bar (Loop, Metro | Stop, Play, Rec | Vol, Export) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Group 1: Timing toggles (Loop & Metronome)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isLooping = state.currentProject?.isLooping == true
                    FilledTonalIconButton(
                        onClick = onToggleLoop,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isLooping) StudioCyan.copy(alpha = 0.25f) else StudioSurfaceElevated,
                            contentColor = if (isLooping) StudioCyan else TextSecondary
                        ),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("loop_button")
                    ) {
                        Icon(Icons.Default.Repeat, contentDescription = "Loop", modifier = Modifier.size(16.dp))
                    }

                    val isMetro = state.isMetronomeOn
                    FilledTonalIconButton(
                        onClick = onToggleMetronome,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isMetro) StudioAmber.copy(alpha = 0.25f) else StudioSurfaceElevated,
                            contentColor = if (isMetro) StudioAmber else TextSecondary
                        ),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("metronome_button")
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = "Metronome", modifier = Modifier.size(16.dp))
                    }
                }

                // Group 2: Core Transport Controls (Stop, Play/Pause Hero, Record) - Perfectly Centered & Aligned
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Stop Button
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(36.dp)
                            .background(StudioSurfaceElevated, CircleShape)
                            .border(1.dp, StudioBorder, CircleShape)
                            .testTag("stop_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = TextPrimary, modifier = Modifier.size(16.dp))
                    }

                    // Play / Pause Hero Button
                    val isPlaying = state.isPlaying
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                Brush.linearGradient(
                                    if (isPlaying) listOf(StudioCyan, StudioViolet)
                                    else listOf(StudioCyan.copy(alpha = 0.9f), StudioViolet.copy(alpha = 0.9f))
                                ),
                                CircleShape
                            )
                            .testTag("play_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = StudioDarkBg,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Record Button with smooth pulse animation
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
                            .size(36.dp)
                            .background(
                                if (isRec) StudioRedRecord.copy(alpha = recAlpha) else StudioSurfaceElevated,
                                CircleShape
                            )
                            .border(1.dp, if (isRec) StudioRedRecord else StudioBorder, CircleShape)
                            .testTag("record_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isRec) Color.White else StudioRedRecord)
                        )
                    }
                }

                // Group 3: Master Volume Popout & Export Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Volume Control Trigger
                    Box {
                        FilledTonalIconButton(
                            onClick = { showVolumePopup = !showVolumePopup },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (showVolumePopup) StudioCyan.copy(alpha = 0.25f) else StudioSurfaceElevated,
                                contentColor = if (showVolumePopup) StudioCyan else TextSecondary
                            ),
                            modifier = Modifier.size(36.dp).testTag("master_volume_button")
                        ) {
                            Icon(
                                imageVector = if (state.masterVolume > 0.5f) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                contentDescription = "Master Volume",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Floating Volume Slider Popout (Never collides with transport bar)
                        if (showVolumePopup) {
                            Popup(
                                alignment = Alignment.BottomEnd,
                                onDismissRequest = { showVolumePopup = false }
                            ) {
                                Surface(
                                    color = StudioSurfaceElevated,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, StudioBorder),
                                    shadowElevation = 8.dp,
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .width(170.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "主音量 (Master)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "${(state.masterVolume * 100).roundToInt()}%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StudioCyan
                                            )
                                        }
                                        Slider(
                                            value = state.masterVolume,
                                            onValueChange = onVolumeChange,
                                            colors = SliderDefaults.colors(
                                                thumbColor = StudioCyan,
                                                activeTrackColor = StudioCyan,
                                                inactiveTrackColor = StudioBorder
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Export Master Audio Action
                    FilledTonalIconButton(
                        onClick = onExportAudio,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = StudioCyan.copy(alpha = 0.2f),
                            contentColor = StudioCyan
                        ),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("top_bar_export_audio_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export Audio", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    // BPM & Time Signature Tuning Dialog
    if (showBpmDialog) {
        var tempBpm by remember { mutableIntStateOf(state.currentProject?.bpm ?: 120) }
        AlertDialog(
            onDismissRequest = { showBpmDialog = false },
            title = {
                Text(
                    text = strings.bpmDialogTitle,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "$tempBpm BPM",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StudioAmber
                    )

                    Slider(
                        value = tempBpm.toFloat(),
                        onValueChange = { tempBpm = it.toInt() },
                        valueRange = 40f..240f,
                        colors = SliderDefaults.colors(
                            thumbColor = StudioAmber,
                            activeTrackColor = StudioAmber,
                            inactiveTrackColor = StudioBorder
                        )
                    )

                    // Quick Preset BPM Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(80, 100, 120, 128, 140).forEach { presetBpm ->
                            Surface(
                                color = if (tempBpm == presetBpm) StudioAmber else StudioSurfaceElevated,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, StudioBorder),
                                modifier = Modifier.clickable { tempBpm = presetBpm }
                            ) {
                                Text(
                                    text = "$presetBpm",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tempBpm == presetBpm) StudioDarkBg else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBpmChange(tempBpm)
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

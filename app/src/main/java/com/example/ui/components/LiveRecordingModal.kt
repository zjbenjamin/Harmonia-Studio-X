package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.AudioRecordManager
import com.example.audio.InstrumentType
import com.example.ui.theme.*
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.delay
import java.io.File
import kotlin.random.Random

enum class RecordingSourceType {
    MICROPHONE_AUDIO,
    VIRTUAL_INSTRUMENT_MIDI
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRecordingModal(
    state: StudioUiState,
    viewModel: StudioViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sourceType by remember { mutableStateOf(RecordingSourceType.MICROPHONE_AUDIO) }
    var trackNameInput by remember { mutableStateOf("Vocal & Instrument Take 1") }

    val audioRecordManager = remember { AudioRecordManager(context) }
    val recordState by audioRecordManager.recordingState.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (!isGranted) {
            viewModel.showToast("录音需要麦克风权限")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (recordState.isRecording) {
                audioRecordManager.stopRecording()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!recordState.isRecording && !state.isRecording) onDismiss()
        },
        containerColor = StudioSurface,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = StudioBorder) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StudioRedRecord.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = StudioRedRecord, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "音频录音与实时 MIDI 录制中心",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "录制真实麦克风声音或捕获触控琴键演奏",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(
                    onClick = {
                        if (recordState.isRecording) audioRecordManager.stopRecording()
                        if (state.isRecording) viewModel.stopPlayback()
                        onDismiss()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            // Mode Selector: Microphone vs Virtual MIDI
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = sourceType == RecordingSourceType.MICROPHONE_AUDIO,
                    onClick = { sourceType = RecordingSourceType.MICROPHONE_AUDIO },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("麦克风人声/乐器音频录制", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                SegmentedButton(
                    selected = sourceType == RecordingSourceType.VIRTUAL_INSTRUMENT_MIDI,
                    onClick = { sourceType = RecordingSourceType.VIRTUAL_INSTRUMENT_MIDI },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Icon(Icons.Default.Piano, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("虚拟键盘 / MIDI 录制", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Mode 1: Microphone Audio Recording
            if (sourceType == RecordingSourceType.MICROPHONE_AUDIO) {
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (recordState.isRecording) StudioRedRecord else StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Timer & Pulsing REC Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val infiniteTransition = rememberInfiniteTransition(label = "rec_ring")
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(tween(500), repeatMode = RepeatMode.Reverse),
                                    label = "rec_blink"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (recordState.isRecording) StudioRedRecord.copy(alpha = alpha) else TextMuted)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (recordState.isRecording) "正在录音 (RECORDING)" else "等待就绪 (STANDBY)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (recordState.isRecording) StudioRedRecord else TextMuted
                                )
                            }

                            val mins = (recordState.durationSeconds / 60).toInt()
                            val secs = (recordState.durationSeconds % 60).toInt()
                            val millis = ((recordState.durationSeconds - (recordState.durationSeconds.toInt())) * 10).toInt()
                            Text(
                                text = String.format("%02d:%02d.%d", mins, secs, millis),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (recordState.isRecording) StudioCyan else TextSecondary
                            )
                        }

                        // Live Audio Oscilloscope & dB Meter
                        Surface(
                            color = StudioDarkBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dbLevel = recordState.currentDbLevel
                                val normalizedLevel = ((dbLevel + 60f) / 60f).coerceIn(0.05f, 1f)
                                for (i in 0 until 32) {
                                    val randomVar = if (recordState.isRecording) (Random.nextFloat() * 0.3f) else 0f
                                    val barHeight = (normalizedLevel * (0.4f + (i % 5) * 0.12f) + randomVar).coerceIn(0.08f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(barHeight)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                when {
                                                    !recordState.isRecording -> StudioBorder
                                                    i > 26 -> StudioRedRecord
                                                    i > 18 -> StudioAmber
                                                    else -> StudioEmerald
                                                }
                                            )
                                    )
                                }
                            }
                        }

                        // Track Name Input
                        OutlinedTextField(
                            value = trackNameInput,
                            onValueChange = { trackNameInput = it },
                            label = { Text("录音音轨名称", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Main Record Button
                        if (!recordState.isRecording) {
                            Button(
                                onClick = {
                                    if (!hasMicPermission) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        val started = audioRecordManager.startRecording()
                                        if (!started) {
                                            viewModel.showToast("无法启动麦克风录音设备")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("start_audio_rec_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = StudioRedRecord),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("点击开始麦克风录音", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    val file = audioRecordManager.stopRecording()
                                    if (file != null && file.exists()) {
                                        // Create new track with recorded audio clip
                                        val projId = state.currentProject?.id
                                        if (projId != null) {
                                            viewModel.createAudioRecordingTrack(
                                                name = trackNameInput.ifBlank { "Microphone Take" },
                                                audioFile = file,
                                                durationSeconds = recordState.durationSeconds
                                            )
                                            viewModel.showToast("录音已成功保存并添加至时间轴！")
                                            onDismiss()
                                        }
                                    } else {
                                        viewModel.showToast("录音保存完成")
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("stop_audio_rec_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, tint = StudioDarkBg)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("完成录音并创建音轨到时间轴", color = StudioDarkBg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            } else {
                // Mode 2: Virtual Instrument MIDI Recording
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (state.isRecording) StudioRedRecord else StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "虚拟乐器与 MIDI 实时录音",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = StudioCyan
                        )
                        Text(
                            text = "开启录音后，在主界面虚拟琴键键盘或打击垫上弹奏，所有音符将与工程节拍器同步录制至时间轴。",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        // Target Track selector
                        Text("录音目标轨道: ${state.tracks.firstOrNull { it.id == state.selectedTrackId }?.name ?: "当前选中轨道"}", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)

                        if (!state.isRecording) {
                            Button(
                                onClick = {
                                    viewModel.toggleRecord()
                                    viewModel.showToast("MIDI 实时录音已开启，请弹奏琴键")
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("start_midi_rec_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = StudioRedRecord),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("开启 MIDI 实时录制并进入琴键", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.toggleRecord()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, tint = StudioDarkBg)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("停止 MIDI 录音并保存", color = StudioDarkBg, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.audio.AudioAnalysisResult
import com.example.audio.AudioDecoder
import com.example.audio.AudioToMidiAnalyzer
import com.example.audio.DecodedAudioData
import com.example.audio.InstrumentType
import com.example.midi.MidiNote
import com.example.ui.theme.*
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioToMidiDialog(
    state: StudioUiState,
    viewModel: StudioViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var sensitivity by remember { mutableFloatStateOf(0.65f) }

    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("") }

    var analysisResult by remember { mutableStateOf<AudioAnalysisResult?>(null) }
    var isAuditioning by remember { mutableStateOf(false) }
    var targetInstrument by remember { mutableStateOf(InstrumentType.GRAND_PIANO) }

    // System Audio File Picker for any audio format
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            val filename = getFileName(context, uri) ?: "Selected_Audio"
            selectedFileName = filename
            analysisResult = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isAnalyzing) onDismiss()
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            .background(StudioCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "音频转 MIDI 智能识别与分析",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "支持 MP3, WAV, AAC, M4A, FLAC, OGG 任意音频转 MIDI",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            // Step 1: Select Audio File
            Surface(
                color = StudioSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. 选择需要分析的音频文件",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioCyan,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pick from device button
                        Button(
                            onClick = { audioPickerLauncher.launch("audio/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, tint = StudioDarkBg, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("浏览本地音频文件", color = StudioDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Preset demo sample
                        OutlinedButton(
                            onClick = {
                                selectedFileName = "Demo_Vocal_Melody.wav"
                                selectedUri = Uri.parse("preset://demo_vocal")
                                analysisResult = null
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioViolet.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = StudioViolet, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("使用示例音频", fontSize = 11.sp, color = StudioViolet)
                        }
                    }

                    if (selectedFileName != null) {
                        Surface(
                            color = StudioDarkBg,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioEmerald.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioEmerald, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedFileName!!,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                    Text("准备就绪，点击下方按钮开始分析基频与音符", fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // Step 2: Analysis Settings & Run Button
            if (analysisResult == null) {
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("2. 音高捕捉灵敏度", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioAmber)
                            Text("${(sensitivity * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioAmber)
                        }

                        Slider(
                            value = sensitivity,
                            onValueChange = { sensitivity = it },
                            valueRange = 0.2f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = StudioAmber, activeTrackColor = StudioAmber)
                        )

                        // Run Button or Progress Bar
                        if (isAnalyzing) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(statusText, fontSize = 11.sp, color = StudioCyan)
                                    Text("${(analysisProgress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioCyan)
                                }
                                LinearProgressIndicator(
                                    progress = { analysisProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = StudioCyan,
                                    trackColor = StudioSurfaceActive
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (selectedFileName == null) {
                                        viewModel.showToast("请先选择或浏览音频文件")
                                        return@Button
                                    }

                                    isAnalyzing = true
                                    analysisProgress = 0.05f
                                    statusText = "正在解码音频数据并提取 PCM 采样..."

                                    coroutineScope.launch {
                                        try {
                                            val decoded: DecodedAudioData = if (selectedUri?.scheme == "preset") {
                                                // Synthesize sample melody audio for testing
                                                statusText = "生成示例旋律音频并进行分析..."
                                                val sr = 44100
                                                val dur = 4.0f
                                                val totalSamples = (sr * dur).toInt()
                                                val pcm = FloatArray(totalSamples)
                                                val notes = listOf(60 to 0.0f, 62 to 0.5f, 64 to 1.0f, 65 to 1.5f, 67 to 2.0f, 69 to 2.5f, 71 to 3.0f, 72 to 3.5f)
                                                for ((pitch, startSec) in notes) {
                                                    val startIdx = (startSec * sr).toInt()
                                                    val freq = 440.0 * Math.pow(2.0, (pitch - 69) / 12.0)
                                                    val len = (0.45f * sr).toInt()
                                                    for (i in 0 until len) {
                                                        if (startIdx + i < totalSamples) {
                                                            val t = i.toDouble() / sr
                                                            pcm[startIdx + i] += (Math.sin(2.0 * Math.PI * freq * t) * Math.exp(-2.5 * t) * 0.7).toFloat()
                                                        }
                                                    }
                                                }
                                                DecodedAudioData(pcm, sr, 1, dur)
                                            } else {
                                                AudioDecoder.decodeAudioToFloatPcm(context, selectedUri!!) { p ->
                                                    analysisProgress = p
                                                    statusText = "正在多线程解码音频流 (${(p * 100).toInt()}%)..."
                                                }
                                            }

                                            statusText = "正在执行 YIN 基频追踪与音符切分算法..."
                                            val bpm = state.currentProject?.bpm ?: 120
                                            val result = AudioToMidiAnalyzer.analyzeAudioToMidi(
                                                audioData = decoded,
                                                bpm = bpm,
                                                originalFileName = selectedFileName ?: "Audio_Track",
                                                sensitivity = sensitivity
                                            ) { p ->
                                                analysisProgress = p
                                                statusText = "正在识别旋律音符 (${(p * 100).toInt()}%)..."
                                            }

                                            analysisResult = result
                                            isAnalyzing = false
                                            viewModel.showToast("成功识别 ${result.notes.size} 个 MIDI 音符！")
                                        } catch (e: Exception) {
                                            isAnalyzing = false
                                            viewModel.showToast("音频分析失败: ${e.message}")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("start_audio_to_midi_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = StudioDarkBg)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("开始智能分析音频 MIDI", color = StudioDarkBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                // Step 3: Analysis Results, Piano Roll Preview, Audition, Apply to Timeline & Export MIDI
                val res = analysisResult!!

                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "分析完成！已识别 ${res.notes.size} 个 MIDI 音符",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioEmerald
                                )
                                Text(
                                    text = "推测调式: ${res.detectedKey} • 音频时长: ${String.format("%.1f", res.durationSeconds)}s",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            // Re-analyze
                            TextButton(onClick = { analysisResult = null }) {
                                Text("重新分析", color = TextMuted, fontSize = 11.sp)
                            }
                        }

                        // Preview of detected notes as piano roll bars
                        Surface(
                            color = StudioDarkBg,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .padding(vertical = 2.dp)
                        ) {
                            val maxBeat = (res.notes.maxOfOrNull { it.startBeat + it.durationBeats } ?: 16f).coerceAtLeast(8f)
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                res.notes.forEach { note ->
                                    val noteHeight = ((note.pitch - 40).coerceIn(10, 50)).dp
                                    Box(
                                        modifier = Modifier
                                            .width(((note.durationBeats * 24f).coerceAtLeast(16f)).dp)
                                            .height(noteHeight)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(StudioCyan.copy(alpha = note.velocity))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(3.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = note.noteName,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StudioDarkBg
                                        )
                                    }
                                }
                            }
                        }

                        // Instrument Selector for Audition / Timeline creation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("试听与分配乐器:", fontSize = 11.sp, color = TextSecondary)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    InstrumentType.GRAND_PIANO to "大钢琴",
                                    InstrumentType.CHINESE_GUZHENG to "古筝",
                                    InstrumentType.CHINESE_PIPA to "琵琶",
                                    InstrumentType.CHINESE_DIZI to "竹笛",
                                    InstrumentType.SYNTH_POLY_KEYS to "合成器"
                                ).forEach { (inst, label) ->
                                    val isSelected = targetInstrument == inst
                                    Surface(
                                        color = if (isSelected) StudioCyan else StudioSurfaceActive,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.clickable { targetInstrument = inst }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) StudioDarkBg else TextPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Action Buttons: Audition, Apply to Timeline, Export .mid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Audition Button
                            OutlinedButton(
                                onClick = {
                                    if (isAuditioning) {
                                        isAuditioning = false
                                    } else {
                                        isAuditioning = true
                                        coroutineScope.launch {
                                            for (note in res.notes) {
                                                if (!isAuditioning) break
                                                viewModel.audioEngine.noteOn(note.pitch, note.velocity, targetInstrument, 9999L)
                                                kotlinx.coroutines.delay((note.durationBeats * 250).toLong().coerceIn(60, 600))
                                                viewModel.audioEngine.noteOff(note.pitch, 9999L)
                                            }
                                            isAuditioning = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyan),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAuditioning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = StudioCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isAuditioning) "停止试听" else "试听 MIDI", fontSize = 11.sp, color = StudioCyan)
                            }

                            // Export Standard MIDI File (.mid)
                            Button(
                                onClick = {
                                    val midiFile = File(context.cacheDir, "${res.originalFileName.substringBeforeLast('.')}_Analyzed.mid")
                                    val bpm = state.currentProject?.bpm ?: 120
                                    val success = AudioToMidiAnalyzer.exportMidiFile(res.notes, midiFile, bpm)
                                    if (success) {
                                        try {
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", midiFile)
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "audio/midi"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "导出 MIDI 文件"))
                                            viewModel.showToast("已成功导出 MIDI 文件！")
                                        } catch (e: Exception) {
                                            viewModel.showToast("导出保存成功: ${midiFile.name}")
                                        }
                                    } else {
                                        viewModel.showToast("导出 MIDI 文件失败")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = StudioAmber),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = StudioDarkBg, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("导出 .mid 文件", color = StudioDarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        // Apply to Timeline
                        Button(
                            onClick = {
                                val projId = state.currentProject?.id
                                if (projId != null) {
                                    viewModel.addNewTrackWithNotes(
                                        name = "${res.originalFileName.substringBeforeLast('.')} (MIDI)",
                                        instrument = targetInstrument,
                                        notes = res.notes
                                    )
                                    viewModel.showToast("已导入为新轨道 (${res.notes.size} 音符)！")
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("apply_analyzed_midi_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, tint = StudioDarkBg)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("添加为新轨道并应用到时间轴", color = StudioDarkBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = it.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

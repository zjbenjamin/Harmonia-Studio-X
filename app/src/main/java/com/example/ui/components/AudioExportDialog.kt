package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import androidx.compose.animation.*
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
import androidx.core.content.FileProvider
import com.example.audio.AudioEngine
import com.example.audio.InstrumentType
import com.example.data.StudioRepository
import com.example.ui.theme.*
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class ExportAudioFormat(val displayName: String, val extension: String, val description: String) {
    WAV("WAV (Lossless)", "wav", "16-bit 44.1kHz Stereo PCM Studio Master"),
    MP3("MP3 Audio", "mp3", "Universal audio format for media players & sharing")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioExportDialog(
    state: StudioUiState,
    viewModel: StudioViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedFormat by remember { mutableStateOf(ExportAudioFormat.WAV) }
    var normalizeAudio by remember { mutableStateOf(true) }
    var includeTail by remember { mutableStateOf(true) }

    var isRendering by remember { mutableStateOf(false) }
    var renderProgress by remember { mutableFloatStateOf(0f) }
    var exportedFile by remember { mutableStateOf<File?>(null) }
    var statusText by remember { mutableStateOf("") }

    // Media preview player
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isRendering) {
                mediaPlayer?.stop()
                onDismiss()
            }
        },
        containerColor = StudioSurface,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = StudioBorder) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Export Audio",
                        tint = StudioCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Export Project Audio",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "${state.currentProject?.title} (${state.currentProject?.bpm} BPM)",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                if (!isRendering) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
            }

            HorizontalDivider(color = StudioBorder)

            if (exportedFile == null) {
                // Section 1: Audio Format Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. SELECT EXPORT FORMAT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioCyan,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExportAudioFormat.entries.forEach { format ->
                            val isSelected = selectedFormat == format
                            Surface(
                                color = if (isSelected) StudioCyan.copy(alpha = 0.15f) else StudioSurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) StudioCyan else StudioBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = !isRendering) { selectedFormat = format }
                                    .testTag("export_format_${format.name}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = format.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) StudioCyan else TextPrimary
                                        )
                                        if (isSelected) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = format.description,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Master Audio Processing Options
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "2. MASTER PROCESSING OPTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioViolet,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Peak Normalization Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Peak Normalization (-0.3 dBFS)", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Text("Maximizes dynamic loudness without digital clipping", fontSize = 10.sp, color = TextSecondary)
                                }
                                Switch(
                                    checked = normalizeAudio,
                                    onCheckedChange = { normalizeAudio = it },
                                    enabled = !isRendering,
                                    colors = SwitchDefaults.colors(checkedThumbColor = StudioCyan, checkedTrackColor = StudioSurfaceActive)
                                )
                            }

                            HorizontalDivider(color = StudioBorder.copy(alpha = 0.5f))

                            // Reverb Tail Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Include Natural Reverb Tail (+2.0s)", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Text("Prevents abrupt ending cuts for decaying synths and cymbals", fontSize = 10.sp, color = TextSecondary)
                                }
                                Switch(
                                    checked = includeTail,
                                    onCheckedChange = { includeTail = it },
                                    enabled = !isRendering,
                                    colors = SwitchDefaults.colors(checkedThumbColor = StudioAmber, checkedTrackColor = StudioSurfaceActive)
                                )
                            }
                        }
                    }
                }

                // Rendering Progress or Start Button
                if (isRendering) {
                    Surface(
                        color = StudioSurfaceActive,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(statusText, fontSize = 12.sp, color = StudioCyan, fontWeight = FontWeight.SemiBold)
                                Text("${(renderProgress * 100).toInt()}%", fontSize = 12.sp, color = StudioCyan, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { renderProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = StudioCyan,
                                trackColor = StudioBorder
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            isRendering = true
                            renderProgress = 0.05f
                            statusText = "Synthesizing multi-track voices..."

                            coroutineScope.launch(Dispatchers.Default) {
                                val proj = state.currentProject ?: return@launch
                                val tracksMap = state.tracks.associateBy { it.id }

                                val exportNotes = mutableListOf<AudioEngine.ExportNoteEvent>()
                                for (clip in state.clips) {
                                    val track = tracksMap[clip.trackId] ?: continue
                                    if (track.isMuted) continue
                                    val inst = InstrumentType.fromId(track.instrumentType)
                                    val notes = StudioRepository(com.example.data.StudioDatabase.getInstance(context).studioDao())
                                        .deserializeNotes(clip.notesJson)

                                    for (n in notes) {
                                        exportNotes.add(
                                            AudioEngine.ExportNoteEvent(
                                                midiNote = n.pitch,
                                                startBeat = clip.startBeat + n.startBeat,
                                                durationBeats = n.durationBeats,
                                                velocity = n.velocity,
                                                instrument = inst,
                                                trackVolume = track.volume,
                                                trackPan = track.pan
                                            )
                                        )
                                    }
                                }

                                val totalBeats = proj.loopEndBeat
                                val tailSecs = if (includeTail) 2.0f else 0.5f
                                val totalSecs = (totalBeats * 60f / proj.bpm) + tailSecs

                                val cleanTitle = proj.title.replace("[^a-zA-Z0-9_]".toRegex(), "_")
                                val targetFileName = "${cleanTitle}.${selectedFormat.extension}"
                                val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.cacheDir
                                val targetFile = File(musicDir, targetFileName)

                                withContext(Dispatchers.Main) {
                                    statusText = "Rendering 44.1kHz Stereo PCM stream..."
                                }

                                val success = viewModel.audioEngine.exportWav(
                                    outputFile = targetFile,
                                    totalDurationSeconds = totalSecs,
                                    bpm = proj.bpm,
                                    normalize = normalizeAudio,
                                    onProgress = { prog ->
                                        coroutineScope.launch(Dispatchers.Main) {
                                            renderProgress = prog
                                        }
                                    }
                                ) {
                                    exportNotes
                                }

                                withContext(Dispatchers.Main) {
                                    isRendering = false
                                    if (success && targetFile.exists()) {
                                        exportedFile = targetFile
                                        statusText = "Render complete!"
                                        // Initialize preview media player
                                        try {
                                            val mp = MediaPlayer()
                                            mp.setDataSource(targetFile.absolutePath)
                                            mp.prepare()
                                            mp.setOnCompletionListener { isPreviewPlaying = false }
                                            mediaPlayer = mp
                                        } catch (_: Exception) {}
                                    } else {
                                        viewModel.showToast("Failed to render audio")
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_audio_render_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = StudioDarkBg)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Render & Export ${selectedFormat.displayName}",
                            color = StudioDarkBg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // Section 3: Render Complete! In-App Player & Sharing Options
                val file = exportedFile!!
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(StudioEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioEmerald, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text(
                                    text = "Size: ${(file.length() / 1024)} KB • Stereo 44.1kHz 16-bit",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // In-App Preview Player
                        Surface(
                            color = StudioDarkBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        val mp = mediaPlayer
                                        if (mp != null) {
                                            if (mp.isPlaying) {
                                                mp.pause()
                                                isPreviewPlaying = false
                                            } else {
                                                mp.start()
                                                isPreviewPlaying = true
                                            }
                                        }
                                    },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = StudioCyan,
                                        contentColor = StudioDarkBg
                                    ),
                                    modifier = Modifier.size(38.dp).testTag("preview_player_play_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPreviewPlaying) "Pause" else "Play Preview",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Preview Master Audio", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(
                                        text = if (isPreviewPlaying) "Playing in-app preview..." else "Tap to audition rendered audio",
                                        fontSize = 10.sp,
                                        color = if (isPreviewPlaying) StudioCyan else TextMuted
                                    )
                                }
                            }
                        }

                        // Share / External Playback Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Share via Intent
                            Button(
                                onClick = {
                                    try {
                                        val uri: Uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val mime = if (file.name.endsWith(".mp3")) "audio/mp3" else "audio/wav"
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = mime
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Music File"))
                                    } catch (e: Exception) {
                                        viewModel.showToast("Sharing failed: ${e.message}")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = StudioDarkBg, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Audio", color = StudioDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            // Done
                            OutlinedButton(
                                onClick = {
                                    mediaPlayer?.stop()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Done", color = TextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.StudioI18n
import com.example.ui.theme.*
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CollaborationView(
    state: StudioUiState,
    viewModel: StudioViewModel,
    onOpenAudioExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = StudioI18n.getStrings(state.language)
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showExportedJsonDialog by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 0: Language Settings / 语言设置
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth().testTag("language_settings_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = StudioViolet, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.languageSetting,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        AppLanguage.SIMPLIFIED_CHINESE to "简体中文 (Chinese)",
                        AppLanguage.ENGLISH to "English"
                    ).forEach { (lang, label) ->
                        val isSelected = state.language == lang
                        Surface(
                            color = if (isSelected) StudioViolet.copy(alpha = 0.2f) else StudioSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) StudioViolet else StudioBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setLanguage(lang) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = StudioViolet, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) StudioViolet else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 1: Cloud Sync & Room Code Card
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.cloudWorkspaceTitle,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StudioEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${strings.statusLabel}: ${state.currentProject?.cloudSyncStatus ?: "Synced"} (Rev #${state.currentProject?.syncRevision ?: 1})",
                                color = StudioEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Sync Now Button
                    Button(
                        onClick = { viewModel.syncProjectNow() },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("sync_now_button")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = StudioDarkBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.syncNow, color = StudioDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Share Session Code
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(strings.projectShareCode, fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text(
                                text = state.currentProject?.shareCode ?: "HRMN-9042",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StudioCyan
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Share Code", state.currentProject?.shareCode ?: "HRMN-9042")
                                clipboard.setPrimaryClip(clip)
                                viewModel.showToast("Copied share code to clipboard")
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyan)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.copyCode, color = StudioCyan, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Section 2: Connected Collaborators & Devices
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${strings.activeCollaborators} (${state.collaborators.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioViolet,
                    letterSpacing = 1.sp
                )

                state.collaborators.forEach { collab ->
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(collab.avatarColorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = collab.name.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = StudioDarkBg,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(collab.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("(${collab.deviceName})", fontSize = 10.sp, color = TextMuted)
                                }
                                Text(collab.currentActivity, fontSize = 11.sp, color = TextSecondary)
                            }
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (collab.isOnline) StudioEmerald else TextMuted)
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Cross-Platform File Interchange & Mixdown
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.crossPlatformInterchange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioAmber,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export MIDI File (.mid)
                    Button(
                        onClick = { viewModel.exportMidiFile(context) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(strings.exportMidiFile, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("DAW Multi-track", fontSize = 9.sp, color = TextMuted)
                        }
                    }

                    // Render Mixdown WAV/MP3 Audio
                    Button(
                        onClick = onOpenAudioExport,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(strings.renderAudioFile, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("44.1kHz 16-bit Master", fontSize = 9.sp, color = TextMuted)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export Project JSON Bundle
                    OutlinedButton(
                        onClick = {
                            val json = viewModel.exportProjectJson()
                            showExportedJsonDialog = json
                        },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioViolet.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = StudioViolet, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.exportProjectJson, fontSize = 11.sp, color = StudioViolet)
                    }

                    // Import Project JSON Bundle
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioEmerald.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = StudioEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.importProjectJson, fontSize = 11.sp, color = StudioEmerald)
                    }
                }
            }
        }

        // Section 4: Version Revision History Timeline
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.revisionHistory,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioCoral,
                    letterSpacing = 1.sp
                )

                state.revisions.forEach { rev ->
                    val dateFormatted = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(rev.timestamp))
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(StudioViolet.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("#${rev.revisionNumber}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioViolet)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rev.message, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text("${rev.author} • $dateFormatted", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }

    // Exported JSON Dialog
    if (showExportedJsonDialog != null) {
        val json = showExportedJsonDialog!!
        AlertDialog(
            onDismissRequest = { showExportedJsonDialog = null },
            title = { Text("Project JSON Exchange Bundle", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Copy this bundle to sync with Mac, Windows, iOS or Web DAW:", fontSize = 12.sp, color = TextSecondary)
                    Surface(
                        color = StudioDarkBg,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                    ) {
                        Text(
                            text = json,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = StudioCyan,
                            modifier = Modifier
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Harmonia Project JSON", json))
                        viewModel.showToast("Copied JSON bundle to clipboard")
                        showExportedJsonDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan)
                ) {
                    Text("Copy to Clipboard", color = StudioDarkBg)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportedJsonDialog = null }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = StudioSurface
        )
    }

    // Import JSON Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Remote Project JSON", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste a Harmonia Studio JSON bundle below:", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { Text("{\"app\": \"Harmonia Studio\"...}", fontSize = 11.sp, color = TextMuted) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            viewModel.importProjectBundle(importJsonText)
                            showImportDialog = false
                            importJsonText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald)
                ) {
                    Text("Import & Open", color = StudioDarkBg)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = StudioSurface
        )
    }
}

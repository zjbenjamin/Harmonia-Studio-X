package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AboutModal
import com.example.ui.components.AudioExportDialog
import com.example.ui.components.ChordGeneratorModal
import com.example.ui.components.LiveRecordingModal
import com.example.ui.components.TopTransportBar
import com.example.ui.theme.*
import com.example.ui.views.*
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewTab
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.delay

@Composable
fun StudioMainScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showChordModal by remember { mutableStateOf(false) }
    var showAudioExportModal by remember { mutableStateOf(false) }
    var showLiveRecordingModal by remember { mutableStateOf(false) }
    var showAboutModal by remember { mutableStateOf(false) }

    // Back handler to navigate back to Arranger if on sub-screens
    BackHandler(enabled = state.currentTab != StudioViewTab.ARRANGER) {
        viewModel.setTab(StudioViewTab.ARRANGER)
    }

    // Auto-dismiss status toast after 3 seconds
    LaunchedEffect(state.statusMessage) {
        if (state.statusMessage != null) {
            delay(3000)
            viewModel.clearToast()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
            .statusBarsPadding(),
        containerColor = StudioDarkBg,
        topBar = {
            TopTransportBar(
                state = state,
                onTogglePlay = { viewModel.togglePlay() },
                onToggleRecord = {
                    if (state.isRecording) {
                        viewModel.toggleRecord()
                    } else {
                        showLiveRecordingModal = true
                    }
                },
                onStop = { viewModel.stopPlayback() },
                onToggleLoop = { viewModel.toggleLoop() },
                onToggleMetronome = { viewModel.toggleMetronome() },
                onBpmChange = { viewModel.setBpm(it) },
                onVolumeChange = { viewModel.setMasterVolume(it) },
                onOpenCollab = { viewModel.setTab(StudioViewTab.COLLAB) },
                onExportAudio = { showAudioExportModal = true },
                onToggleLanguage = { viewModel.toggleLanguage() },
                onOpenAbout = { showAboutModal = true }
            )
        },
        bottomBar = {
            StudioBottomNavBar(
                currentTab = state.currentTab,
                language = state.language,
                onTabSelect = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main view switcher
            when (state.currentTab) {
                StudioViewTab.ARRANGER -> {
                    ArrangerView(
                        state = state,
                        viewModel = viewModel,
                        onOpenChordGenerator = { showChordModal = true },
                        onOpenAudioExport = { showAudioExportModal = true }
                    )
                }
                StudioViewTab.PIANO_ROLL -> {
                    PianoRollView(
                        state = state,
                        viewModel = viewModel,
                        onOpenChordGenerator = { showChordModal = true }
                    )
                }
                StudioViewTab.BEAT_SEQUENCER -> {
                    BeatSequencerView(
                        state = state,
                        viewModel = viewModel
                    )
                }
                StudioViewTab.CONTROLLER -> {
                    VirtualControllerView(
                        state = state,
                        viewModel = viewModel
                    )
                }
                StudioViewTab.SOUND_LIBRARY -> {
                    SoundLibraryView(
                        state = state,
                        viewModel = viewModel
                    )
                }
                StudioViewTab.COLLAB -> {
                    CollaborationView(
                        state = state,
                        viewModel = viewModel,
                        onOpenAudioExport = { showAudioExportModal = true },
                        onOpenAbout = { showAboutModal = true }
                    )
                }
            }

            // Status message toast banner
            AnimatedVisibility(
                visible = state.statusMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            ) {
                Surface(
                    color = StudioSurfaceActive,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, StudioCyan),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = state.statusMessage ?: "",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Smart Chord Progression Generator Sheet
    if (showChordModal) {
        ChordGeneratorModal(
            state = state,
            viewModel = viewModel,
            onDismiss = { showChordModal = false }
        )
    }

    // Audio Export Modal (WAV & MP3)
    if (showAudioExportModal) {
        AudioExportDialog(
            state = state,
            viewModel = viewModel,
            onDismiss = { showAudioExportModal = false }
        )
    }

    // Live Recording Modal (Microphone Vocal/Take & MIDI)
    if (showLiveRecordingModal) {
        LiveRecordingModal(
            state = state,
            viewModel = viewModel,
            onDismiss = { showLiveRecordingModal = false }
        )
    }

    // About & Version Update History Modal
    if (showAboutModal) {
        AboutModal(
            onDismiss = { showAboutModal = false }
        )
    }
}

@Composable
private fun StudioBottomNavBar(
    currentTab: StudioViewTab,
    language: com.example.ui.i18n.AppLanguage,
    onTabSelect: (StudioViewTab) -> Unit
) {
    val strings = com.example.ui.i18n.StudioI18n.getStrings(language)

    Surface(
        color = StudioSurface,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf(
                StudioTabItem(StudioViewTab.ARRANGER, strings.tabArranger, Icons.Default.ViewAgenda),
                StudioTabItem(StudioViewTab.PIANO_ROLL, strings.tabPianoRoll, Icons.Default.Piano),
                StudioTabItem(StudioViewTab.BEAT_SEQUENCER, strings.tabBeatSequencer, Icons.Default.GridOn),
                StudioTabItem(StudioViewTab.CONTROLLER, strings.tabController, Icons.Default.Keyboard),
                StudioTabItem(StudioViewTab.SOUND_LIBRARY, strings.tabSoundLibrary, Icons.Default.Audiotrack),
                StudioTabItem(StudioViewTab.COLLAB, strings.tabCollab, Icons.Default.CloudSync)
            )

            tabs.forEach { item ->
                val isSelected = currentTab == item.tab
                val tabColor = when (item.tab) {
                    StudioViewTab.ARRANGER -> StudioCyan
                    StudioViewTab.PIANO_ROLL -> StudioViolet
                    StudioViewTab.BEAT_SEQUENCER -> StudioEmerald
                    StudioViewTab.CONTROLLER -> StudioCoral
                    StudioViewTab.SOUND_LIBRARY -> StudioAmber
                    StudioViewTab.COLLAB -> StudioPurple
                }

                Surface(
                    color = if (isSelected) tabColor.copy(alpha = 0.16f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isSelected) BorderStroke(1.dp, tabColor.copy(alpha = 0.5f)) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelect(item.tab) }
                        .padding(horizontal = 2.dp)
                        .testTag("tab_${item.tab.name}")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) tabColor else TextMuted,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) tabColor else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private data class StudioTabItem(
    val tab: StudioViewTab,
    val label: String,
    val icon: ImageVector
)

package com.example.ui.views

import android.view.MotionEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.InstrumentType
import com.example.midi.MusicalScale
import com.example.ui.theme.*
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel

enum class ControllerMode {
    PIANO,
    DRUM_PADS
}

@Composable
fun VirtualControllerView(
    state: StudioUiState,
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(ControllerMode.PIANO) }
    var currentOctave by remember { mutableIntStateOf(4) } // C4 default
    var isSustainOn by remember { mutableStateOf(false) }
    var scaleLock by remember { mutableStateOf<MusicalScale?>(null) }

    val activeTrack = state.tracks.firstOrNull { it.id == state.selectedTrackId }
    val instrument = if (activeTrack != null) InstrumentType.fromId(activeTrack.instrumentType) else InstrumentType.GRAND_PIANO

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mode Switcher & Octave Header
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Switch between Keys and Drum Pads
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = mode == ControllerMode.PIANO,
                        onClick = { mode = ControllerMode.PIANO },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Keys", fontSize = 11.sp)
                    }
                    SegmentedButton(
                        selected = mode == ControllerMode.DRUM_PADS,
                        onClick = { mode = ControllerMode.DRUM_PADS },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("16 Drum Pads", fontSize = 11.sp)
                    }
                }

                if (mode == ControllerMode.PIANO) {
                    // Octave Shift Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Octave", fontSize = 10.sp, color = TextMuted)
                        FilledTonalIconButton(
                            onClick = { if (currentOctave > 1) currentOctave-- },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text("C$currentOctave", color = StudioCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        FilledTonalIconButton(
                            onClick = { if (currentOctave < 7) currentOctave++ },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    // Sustain Pedal Toggle
                    FilterChip(
                        selected = isSustainOn,
                        onClick = { isSustainOn = !isSustainOn },
                        label = { Text("Sustain", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioAmber,
                            selectedLabelColor = StudioDarkBg
                        )
                    )
                }
            }
        }

        // Active Instrument indicator
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(instrument.defaultColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Track: ${activeTrack?.name ?: "Master"} (${instrument.displayName})",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (state.isRecording) {
                Text("● REC LIVE MIDI", color = StudioRedRecord, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Main Controller Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (mode == ControllerMode.PIANO) {
                PianoKeyboardControl(
                    baseOctave = currentOctave,
                    isSustain = isSustainOn,
                    onNoteDown = { pitch -> viewModel.onLiveNoteDown(pitch) },
                    onNoteUp = { pitch -> if (!isSustainOn) viewModel.onLiveNoteUp(pitch) }
                )
            } else {
                DrumPads16Grid(
                    onDrumTrigger = { drumNote -> viewModel.onLiveDrumTrigger(drumNote) }
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PianoKeyboardControl(
    baseOctave: Int,
    isSustain: Boolean,
    onNoteDown: (Int) -> Unit,
    onNoteUp: (Int) -> Unit
) {
    // Render 14 white keys spanning 2 octaves (e.g. C4 to B5)
    val startNote = (baseOctave + 1) * 12 // e.g. 60 for C4
    val whiteKeyOffsets = listOf(0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23)
    val whiteKeyNames = listOf("C", "D", "E", "F", "G", "A", "B", "C", "D", "E", "F", "G", "A", "B")

    // Active pressed keys tracking
    val pressedKeys = remember { mutableStateListOf<Int>() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioDarkBg)
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
    ) {
        val totalWhiteKeys = whiteKeyOffsets.size
        val whiteKeyWidth = maxWidth / totalWhiteKeys

        // 1. White Keys Row
        Row(modifier = Modifier.fillMaxSize()) {
            for (i in 0 until totalWhiteKeys) {
                val pitch = startNote + whiteKeyOffsets[i]
                val isPressed = pitch in pressedKeys

                Box(
                    modifier = Modifier
                        .width(whiteKeyWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 0.5.dp)
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(if (isPressed) StudioCyan else Color.White)
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .pointerInteropFilter { motionEvent ->
                            when (motionEvent.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    if (pitch !in pressedKeys) pressedKeys.add(pitch)
                                    onNoteDown(pitch)
                                    true
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    pressedKeys.remove(pitch)
                                    onNoteUp(pitch)
                                    true
                                }
                                else -> false
                            }
                        }
                        .testTag("key_white_$pitch"),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = whiteKeyNames[i],
                        color = if (isPressed) StudioDarkBg else Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        // 2. Black Keys Layer
        val blackKeyOffsets = listOf(
            0 to 1,   // C#
            1 to 3,   // D#
            3 to 6,   // F#
            4 to 8,   // G#
            5 to 10,  // A#
            7 to 13,  // C# (next octave)
            8 to 15,  // D#
            10 to 18, // F#
            11 to 20, // G#
            12 to 22  // A#
        )
        val blackKeyWidth = whiteKeyWidth * 0.65f
        val blackKeyHeight = maxHeight * 0.60f

        for ((whiteIndex, semitone) in blackKeyOffsets) {
            val pitch = startNote + semitone
            val isPressed = pitch in pressedKeys
            val leftOffset = (whiteKeyWidth * (whiteIndex + 1)) - (blackKeyWidth / 2)

            Box(
                modifier = Modifier
                    .offset(x = leftOffset)
                    .width(blackKeyWidth)
                    .height(blackKeyHeight)
                    .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                    .background(if (isPressed) StudioViolet else Color(0xFF1E293B))
                    .border(1.dp, Color.Black, RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                    .pointerInteropFilter { motionEvent ->
                        when (motionEvent.action) {
                            MotionEvent.ACTION_DOWN -> {
                                if (pitch !in pressedKeys) pressedKeys.add(pitch)
                                onNoteDown(pitch)
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                pressedKeys.remove(pitch)
                                onNoteUp(pitch)
                                true
                            }
                            else -> false
                        }
                    }
                    .testTag("key_black_$pitch")
            )
        }
    }
}

@Composable
private fun DrumPads16Grid(
    onDrumTrigger: (Int) -> Unit
) {
    val drumPads = listOf(
        Triple("808 Sub", 35, Color(0xFF10B981)),
        Triple("Kick 1", 36, Color(0xFFEF4444)),
        Triple("Kick 2", 35, Color(0xFFF87171)),
        Triple("Snare 1", 38, Color(0xFFF97316)),

        Triple("Snare 2", 40, Color(0xFFFB923C)),
        Triple("Hand Clap", 39, Color(0xFFA855F7)),
        Triple("Rimshot", 37, Color(0xFFC084FC)),
        Triple("Closed Hat", 42, Color(0xFF38BDF8)),

        Triple("Open Hat", 46, Color(0xFF818CF8)),
        Triple("Pedal Hat", 44, Color(0xFF6366F1)),
        Triple("Low Tom", 41, Color(0xFF14B8A6)),
        Triple("Mid Tom", 45, Color(0xFF2DD4BF)),

        Triple("High Tom", 48, Color(0xFF34D399)),
        Triple("Crash", 49, Color(0xFFF59E0B)),
        Triple("Ride", 51, Color(0xFFFBBF24)),
        Triple("Cowbell", 56, Color(0xFFE11D48))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioSurface)
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (row in 0 until 4) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0 until 4) {
                    val padIndex = row * 4 + col
                    val (label, note, color) = drumPads[padIndex]
                    var isPressed by remember { mutableStateOf(false) }

                    Surface(
                        color = if (isPressed) color else color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isPressed) Color.White else color.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isPressed = true
                                        onDrumTrigger(note)
                                        tryAwaitRelease()
                                        isPressed = false
                                    }
                                )
                            }
                            .testTag("drum_pad_$note")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PAD ${padIndex + 1}",
                                fontSize = 9.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = if (isPressed) StudioDarkBg else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isPressed) Color.White else color)
                            )
                        }
                    }
                }
            }
        }
    }
}

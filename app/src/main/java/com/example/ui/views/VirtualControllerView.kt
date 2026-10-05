package com.example.ui.views

import android.view.MotionEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.InstrumentType
import com.example.ui.i18n.StudioI18n
import com.example.ui.theme.*
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.launch

enum class ControllerMode {
    PIANO,
    DRUM_PADS
}

/**
 * Key scale presets for virtual piano keyboard.
 * AUTO_ADAPTIVE dynamically computes key count to guarantee a comfortable 1:4.5 touch aspect ratio
 * regardless of phone portrait, landscape, foldable, or tablet densities.
 */
enum class PianoKeyScale(val keyCount: Int, val shortLabel: String, val desc: String) {
    AUTO_ADAPTIVE(0, "自适应比例", "按屏幕密度与朝向动态维持黄金触控比例"),
    WIDE_8(8, "8键(舒适)", "单八度大琴键，触感宽大防误触"),
    STANDARD_10(10, "10键(标准)", "1.5八度，兼顾跨度与触感"),
    COMPACT_14(14, "14键(紧凑)", "双八度，适合平板或广音域")
}

@Composable
fun VirtualControllerView(
    state: StudioUiState,
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val strings = StudioI18n.getStrings(state.language)
    var mode by remember { mutableStateOf(ControllerMode.PIANO) }
    var currentOctave by remember { mutableIntStateOf(4) } // C4 default
    var keyScale by remember { mutableStateOf(PianoKeyScale.AUTO_ADAPTIVE) } // Default to intelligent dynamic scaling
    var isSustainOn by remember { mutableStateOf(false) }
    var showWheelsInPortrait by remember { mutableStateOf(false) }

    val activeTrack = state.tracks.firstOrNull { it.id == state.selectedTrackId }
    val instrument = if (activeTrack != null) InstrumentType.fromId(activeTrack.instrumentType) else InstrumentType.GRAND_PIANO

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp * 1.25f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // --- 1. Mode Switcher & Octave / Key Proportion Bar ---
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: Mode Switcher + Sustain + Recording status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Switch between Piano Keys and Drum Pads
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.height(34.dp)) {
                        SegmentedButton(
                            selected = mode == ControllerMode.PIANO,
                            onClick = { mode = ControllerMode.PIANO },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Icon(Icons.Default.Piano, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.modeKeys, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        SegmentedButton(
                            selected = mode == ControllerMode.DRUM_PADS,
                            onClick = { mode = ControllerMode.DRUM_PADS },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.modeDrumPads, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (mode == ControllerMode.PIANO) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Wheel Toggle (For portrait mode)
                            if (!isLandscape) {
                                FilterChip(
                                    selected = showWheelsInPortrait,
                                    onClick = { showWheelsInPortrait = !showWheelsInPortrait },
                                    label = { Text("弯音轮", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StudioViolet,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.height(30.dp)
                                )
                            }

                            // Sustain Pedal Toggle
                            FilterChip(
                                selected = isSustainOn,
                                onClick = { isSustainOn = !isSustainOn },
                                label = { Text(strings.sustain, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioAmber,
                                    selectedLabelColor = StudioDarkBg
                                ),
                                modifier = Modifier.height(30.dp)
                            )

                            // Octave Shift Buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                FilledTonalIconButton(
                                    onClick = { if (currentOctave > 1) currentOctave-- },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Surface(
                                    color = StudioSurfaceElevated,
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "C$currentOctave",
                                        color = StudioCyan,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                FilledTonalIconButton(
                                    onClick = { if (currentOctave < 7) currentOctave++ },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }
                    }

                    if (state.isRecording) {
                        Text(strings.recLiveMidi, color = StudioRedRecord, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Row 2 (When in Piano Mode): Proportions & Octave Jump Strip
                if (mode == ControllerMode.PIANO) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Key Proportions / Scale Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("比例:", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            listOf(
                                PianoKeyScale.AUTO_ADAPTIVE to "自适应",
                                PianoKeyScale.WIDE_8 to "8键",
                                PianoKeyScale.STANDARD_10 to "10键",
                                PianoKeyScale.COMPACT_14 to "14键"
                            ).forEach { (scale, label) ->
                                val isSelected = keyScale == scale
                                Surface(
                                    color = if (isSelected) StudioCyan else StudioSurfaceElevated,
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) StudioCyan else StudioBorder
                                    ),
                                    modifier = Modifier
                                        .clickable { keyScale = scale }
                                        .testTag("key_scale_${scale.name.lowercase()}")
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) StudioDarkBg else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Octave Navigation Strip (C1 to C7)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..7).forEach { oct ->
                                val isCur = oct == currentOctave
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isCur) StudioCyan else StudioDarkBg)
                                        .border(
                                            0.5.dp,
                                            if (isCur) Color.White else StudioBorder,
                                            RoundedCornerShape(3.dp)
                                        )
                                        .clickable { currentOctave = oct },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "C$oct",
                                        fontSize = 8.sp,
                                        fontWeight = if (isCur) FontWeight.ExtraBold else FontWeight.Normal,
                                        color = if (isCur) StudioDarkBg else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. Active Instrument & Quick Traditional Chinese Instrument Selector Row ---
        Surface(
            color = StudioSurfaceElevated,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("音色: ", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val quickInstruments = listOf(
                        InstrumentType.GRAND_PIANO to "大钢琴",
                        InstrumentType.CHINESE_GUZHENG to "古筝",
                        InstrumentType.CHINESE_PIPA to "琵琶",
                        InstrumentType.CHINESE_DIZI to "竹笛",
                        InstrumentType.CHINESE_YANGQIN to "扬琴",
                        InstrumentType.CHINESE_SUONA to "唢呐",
                        InstrumentType.CHINESE_GUQIN to "古琴",
                        InstrumentType.CHINESE_BIANZHONG to "编钟",
                        InstrumentType.ERHU to "二胡",
                        InstrumentType.SYNTH_POLY_KEYS to "合成器"
                    )

                    quickInstruments.forEach { (inst, label) ->
                        val isCurrent = instrument == inst
                        Surface(
                            color = if (isCurrent) inst.defaultColor else StudioDarkBg,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isCurrent) Color.White else inst.defaultColor.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.clickable {
                                val trackId = activeTrack?.id
                                if (trackId != null) {
                                    viewModel.assignInstrumentToTrack(trackId, inst)
                                } else {
                                    viewModel.addNewTrack(inst.displayName, inst)
                                }
                            }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) StudioDarkBg else TextPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 3. Main Controller Area: Proportional Piano Keyboard or 16 Drum Pads ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (mode == ControllerMode.PIANO) {
                PianoKeyboardWithDynamicScaling(
                    baseOctave = currentOctave,
                    keyScale = keyScale,
                    isSustain = isSustainOn,
                    activeInstrument = instrument,
                    isLandscape = isLandscape,
                    showWheelsInPortrait = showWheelsInPortrait,
                    onNoteDown = { pitch -> viewModel.onLiveNoteDown(pitch) },
                    onNoteUp = { pitch -> if (!isSustainOn) viewModel.onLiveNoteUp(pitch) },
                    onPitchBendChange = { semitones -> viewModel.setPitchBend(semitones) },
                    onModulationChange = { depth -> viewModel.setModulation(depth) }
                )
            } else {
                DrumPads16Grid(
                    onDrumTrigger = { drumNote -> viewModel.onLiveDrumTrigger(drumNote) }
                )
            }
        }
    }
}

/**
 * Dynamic Scaling Layout Container for Virtual Piano Keyboard.
 * Evaluates screen width, height, density and orientation to ensure keys maintain
 * an ergonomic touch aspect ratio (~1:4.5 to 1:5.0) and touch targets of >= 44dp.
 */
@Composable
private fun PianoKeyboardWithDynamicScaling(
    baseOctave: Int,
    keyScale: PianoKeyScale,
    isSustain: Boolean,
    activeInstrument: InstrumentType,
    isLandscape: Boolean,
    showWheelsInPortrait: Boolean,
    onNoteDown: (Int) -> Unit,
    onNoteUp: (Int) -> Unit,
    onPitchBendChange: (Float) -> Unit,
    onModulationChange: (Float) -> Unit
) {
    val shouldShowWheels = isLandscape || showWheelsInPortrait
    val wheelWidth = if (shouldShowWheels) 64.dp else 0.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioDarkBg)
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
    ) {
        val totalAvailableWidth = maxWidth
        val totalAvailableHeight = maxHeight

        val keyboardAreaWidth = totalAvailableWidth - wheelWidth

        // --- Dynamic Aspect Ratio Calculations ---
        // Target white key touch aspect ratio: width / height ≈ 0.22 (1 : 4.54)
        val TARGET_KEY_ASPECT_RATIO = 0.22f
        val MIN_TOUCH_KEY_WIDTH_DP = 44.dp
        val MAX_TOUCH_KEY_WIDTH_DP = 64.dp

        // 1. Determine key count based on scale mode
        val totalWhiteKeys = when (keyScale) {
            PianoKeyScale.AUTO_ADAPTIVE -> {
                // Dynamically fit comfortable keys that preserve target touch aspect ratio
                val estimatedKeyWidth = (totalAvailableHeight * TARGET_KEY_ASPECT_RATIO).coerceIn(MIN_TOUCH_KEY_WIDTH_DP, MAX_TOUCH_KEY_WIDTH_DP)
                val fitCount = (keyboardAreaWidth / estimatedKeyWidth).toInt()
                if (isLandscape) {
                    fitCount.coerceIn(12, 22) // Wide range in landscape
                } else {
                    fitCount.coerceIn(7, 10)  // Ergonomic range in portrait
                }
            }
            PianoKeyScale.WIDE_8 -> 8
            PianoKeyScale.STANDARD_10 -> 10
            PianoKeyScale.COMPACT_14 -> 14
        }

        // 2. Compute key width and height with aspect ratio clamping
        val unconstrainedKeyWidth = keyboardAreaWidth / totalWhiteKeys
        // Prevent keys from becoming fat blocks in landscape by clamping maximum aspect ratio
        val maxAllowedWidthFromHeight = totalAvailableHeight * 0.30f
        val actualKeyWidth = if (keyScale == PianoKeyScale.AUTO_ADAPTIVE) {
            unconstrainedKeyWidth
        } else {
            unconstrainedKeyWidth.coerceIn(MIN_TOUCH_KEY_WIDTH_DP, maxAllowedWidthFromHeight)
        }

        // Prevent keys from stretching to needle-thin sticks in tall portrait screens
        val idealKeyHeight = actualKeyWidth / TARGET_KEY_ASPECT_RATIO
        val actualKeyHeight = idealKeyHeight.coerceIn(140.dp, totalAvailableHeight)

        val effectiveKeyboardWidth = actualKeyWidth * totalWhiteKeys

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            // Expression Wheels (Pitch Bend & Modulation)
            if (shouldShowWheels) {
                Box(
                    modifier = Modifier
                        .width(wheelWidth)
                        .height(actualKeyHeight)
                        .padding(end = 4.dp)
                ) {
                    PitchBendAndModWheels(
                        height = actualKeyHeight,
                        activeColor = activeInstrument.defaultColor,
                        onPitchBendChange = onPitchBendChange,
                        onModulationChange = onModulationChange
                    )
                }
            }

            // Interactive Piano Keyboard Canvas
            Box(
                modifier = Modifier
                    .width(effectiveKeyboardWidth)
                    .height(actualKeyHeight),
                contentAlignment = Alignment.BottomCenter
            ) {
                PianoKeyboardControl(
                    baseOctave = baseOctave,
                    totalWhiteKeys = totalWhiteKeys,
                    whiteKeyWidth = actualKeyWidth,
                    keyHeight = actualKeyHeight,
                    isSustain = isSustain,
                    activeInstrument = activeInstrument,
                    onNoteDown = onNoteDown,
                    onNoteUp = onNoteUp
                )
            }
        }
    }
}

/**
 * Expressive Pitch Bend and Modulation vertical ribbon wheels.
 */
@Composable
private fun PitchBendAndModWheels(
    height: Dp,
    activeColor: Color,
    onPitchBendChange: (Float) -> Unit,
    onModulationChange: (Float) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    // Pitch Bend: -2 to +2 semitones, auto-spring returns to 0
    val bendAnim = remember { Animatable(0f) }
    // Modulation: 0 to 1, stays where dragged
    var modValue by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(StudioSurface)
            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // 1. Pitch Bend Wheel (Center Spring Return)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(StudioDarkBg)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                bendAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessHigh
                                    )
                                ) {
                                    onPitchBendChange(value)
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val deltaSemitones = -(dragAmount.y / 40f)
                            val newBend = (bendAnim.value + deltaSemitones).coerceIn(-2f, 2f)
                            coroutineScope.launch {
                                bendAnim.snapTo(newBend)
                                onPitchBendChange(newBend)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val bendNorm = (bendAnim.value / 2f) // -1f to +1f
            // Center zero line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(StudioBorder)
            )
            // Interactive bend thumb
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .offset(y = (-bendNorm * 40f).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (bendAnim.value != 0f) activeColor else StudioSurfaceElevated)
                    .border(1.dp, if (bendAnim.value != 0f) Color.White else StudioBorder, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PITCH",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (bendAnim.value != 0f) StudioDarkBg else TextMuted
                )
            }
        }

        // 2. Modulation Wheel (Continuous)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(StudioDarkBg)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val delta = -(dragAmount.y / 100f)
                            modValue = (modValue + delta).coerceIn(0f, 1f)
                            onModulationChange(modValue)
                        }
                    )
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Fill level
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(modValue.coerceAtLeast(0.05f))
                    .background(
                        Brush.verticalGradient(
                            listOf(StudioCyan, StudioCyan.copy(alpha = 0.2f))
                        )
                    )
            )
            Text(
                text = "MOD",
                fontSize = 7.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (modValue > 0.1f) StudioDarkBg else TextMuted,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

/**
 * Mathematically precise Multi-Touch Virtual Piano Keyboard.
 * Generates arbitrary number of keys (from 7 to 24) dynamically while maintaining
 * physical acoustic piano geometry (divides between C/D, D/E, F/G, G/A, A/B).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PianoKeyboardControl(
    baseOctave: Int,
    totalWhiteKeys: Int,
    whiteKeyWidth: Dp,
    keyHeight: Dp,
    isSustain: Boolean,
    activeInstrument: InstrumentType,
    onNoteDown: (Int) -> Unit,
    onNoteUp: (Int) -> Unit
) {
    val density = LocalDensity.current
    val startNote = (baseOctave + 1) * 12

    // Layout configuration generated dynamically for any key count
    val baseSemitones = listOf(0, 2, 4, 5, 7, 9, 11)
    val baseNames = listOf("C", "D", "E", "F", "G", "A", "B")

    val whiteKeyOffsets = remember(totalWhiteKeys) {
        List(totalWhiteKeys) { i ->
            (i / 7) * 12 + baseSemitones[i % 7]
        }
    }

    val whiteKeyNames = remember(totalWhiteKeys) {
        List(totalWhiteKeys) { i ->
            baseNames[i % 7]
        }
    }

    // Black keys sit between C-D(0), D-E(1), F-G(3), G-A(4), A-B(5)
    val blackKeyOffsets = remember(totalWhiteKeys) {
        val list = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until totalWhiteKeys - 1) {
            val noteInOct = i % 7
            val oct = i / 7
            val semitone = when (noteInOct) {
                0 -> (oct * 12) + 1  // C#
                1 -> (oct * 12) + 3  // D#
                3 -> (oct * 12) + 6  // F#
                4 -> (oct * 12) + 8  // G#
                5 -> (oct * 12) + 10 // A#
                else -> null
            }
            if (semitone != null) {
                list.add(i to semitone)
            }
        }
        list
    }

    val pressedKeys = remember { mutableStateListOf<Int>() }
    val pointerToKeyMap = remember { mutableMapOf<Int, Int>() }

    val blackKeyWidth = whiteKeyWidth * 0.60f
    val blackKeyHeight = keyHeight * 0.62f

    val whiteKeyWidthPx = with(density) { whiteKeyWidth.toPx() }
    val blackKeyWidthPx = with(density) { blackKeyWidth.toPx() }
    val blackKeyHeightPx = with(density) { blackKeyHeight.toPx() }

    fun findPitchAt(px: Float, py: Float): Int? {
        // Check black keys first if touch is in upper 62% of keyboard
        if (py in 0f..blackKeyHeightPx) {
            for ((whiteIndex, semitone) in blackKeyOffsets) {
                val center = whiteKeyWidthPx * (whiteIndex + 1)
                val left = center - (blackKeyWidthPx / 2f)
                val right = center + (blackKeyWidthPx / 2f)
                if (px in left..right) {
                    return startNote + semitone
                }
            }
        }
        // Check white keys
        val wIdx = (px / whiteKeyWidthPx).toInt()
        if (wIdx in 0 until totalWhiteKeys) {
            return startNote + whiteKeyOffsets[wIdx]
        }
        return null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInteropFilter { motionEvent ->
                val action = motionEvent.actionMasked
                val actionIndex = motionEvent.actionIndex

                when (action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        val pid = motionEvent.getPointerId(actionIndex)
                        val px = motionEvent.getX(actionIndex)
                        val py = motionEvent.getY(actionIndex)
                        val pitch = findPitchAt(px, py)
                        if (pitch != null) {
                            pointerToKeyMap[pid] = pitch
                            if (pitch !in pressedKeys) {
                                pressedKeys.add(pitch)
                            }
                            onNoteDown(pitch)
                        }
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        for (p in 0 until motionEvent.pointerCount) {
                            val pid = motionEvent.getPointerId(p)
                            val px = motionEvent.getX(p)
                            val py = motionEvent.getY(p)
                            val newPitch = findPitchAt(px, py)
                            val oldPitch = pointerToKeyMap[pid]

                            if (newPitch != null && newPitch != oldPitch) {
                                if (oldPitch != null) {
                                    if (pointerToKeyMap.values.count { it == oldPitch } <= 1) {
                                        pressedKeys.remove(oldPitch)
                                        if (!isSustain) onNoteUp(oldPitch)
                                    }
                                }
                                pointerToKeyMap[pid] = newPitch
                                if (newPitch !in pressedKeys) {
                                    pressedKeys.add(newPitch)
                                }
                                onNoteDown(newPitch)
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_POINTER_UP -> {
                        val pid = motionEvent.getPointerId(actionIndex)
                        val oldPitch = pointerToKeyMap.remove(pid)
                        if (oldPitch != null) {
                            if (oldPitch !in pointerToKeyMap.values) {
                                pressedKeys.remove(oldPitch)
                                if (!isSustain) onNoteUp(oldPitch)
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        for ((_, pitch) in pointerToKeyMap) {
                            if (!isSustain) onNoteUp(pitch)
                        }
                        pointerToKeyMap.clear()
                        pressedKeys.clear()
                        true
                    }
                    else -> false
                }
            }
    ) {
        // 1. White Keys Row
        Row(modifier = Modifier.fillMaxSize()) {
            for (i in 0 until totalWhiteKeys) {
                val semitoneOffset = whiteKeyOffsets[i]
                val pitch = startNote + semitoneOffset
                val isPressed = pitch in pressedKeys
                val isC = semitoneOffset % 12 == 0
                val keyOctave = (pitch / 12) - 1

                Box(
                    modifier = Modifier
                        .width(whiteKeyWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 1.dp)
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(
                            if (isPressed) {
                                Brush.verticalGradient(
                                    listOf(
                                        activeInstrument.defaultColor.copy(alpha = 0.9f),
                                        activeInstrument.defaultColor
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White,
                                        Color(0xFFF8FAFC)
                                    )
                                )
                            }
                        )
                        .border(
                            1.dp,
                            if (isPressed) activeInstrument.defaultColor else Color(0xFFCBD5E1),
                            RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                        )
                        .testTag("key_white_$pitch"),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        if (isC) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isPressed) StudioDarkBg else StudioCyan)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = "${whiteKeyNames[i]}$keyOctave",
                            color = if (isPressed) StudioDarkBg else if (isC) StudioCyan else Color(0xFF475569),
                            fontSize = if (totalWhiteKeys <= 8) 12.sp else 10.sp,
                            fontWeight = if (isPressed || isC) FontWeight.ExtraBold else FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Black Keys Layer
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
                    .background(
                        if (isPressed) {
                            Brush.verticalGradient(
                                listOf(
                                    activeInstrument.defaultColor,
                                    activeInstrument.defaultColor.copy(alpha = 0.8f)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF27272A),
                                    Color(0xFF09090B)
                                )
                            )
                        }
                    )
                    .border(
                        1.dp,
                        if (isPressed) Color.White else Color(0xFF3F3F46),
                        RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
                    )
                    .testTag("key_black_$pitch"),
                contentAlignment = Alignment.BottomCenter
            ) {
                val semitoneName = when (semitone % 12) {
                    1 -> "C#"
                    3 -> "D#"
                    6 -> "F#"
                    8 -> "G#"
                    10 -> "A#"
                    else -> "#"
                }
                Text(
                    text = semitoneName,
                    color = if (isPressed) StudioDarkBg else Color(0xFFA1A1AA),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
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
                        border = BorderStroke(
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

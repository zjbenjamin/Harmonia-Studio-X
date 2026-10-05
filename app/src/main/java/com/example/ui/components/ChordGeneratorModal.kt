package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.midi.*
import com.example.ui.theme.*
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordGeneratorModal(
    state: StudioUiState,
    viewModel: StudioViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = com.example.ui.i18n.StudioI18n.getStrings(state.language)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = StudioBorder) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Smart Chord Generator",
                        tint = StudioAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = strings.chordGeneratorTitle,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = strings.chordGeneratorSubtitle,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            HorizontalDivider(color = StudioBorder)

            // Section 1: Musical Key & Scale
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.sectionKeyScale,
                    color = StudioCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                // Root Notes row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChordProgressionGenerator.ROOT_NOTES.forEach { (keyName, pitch) ->
                        val isSelected = state.chordRootKey == keyName
                        Surface(
                            color = if (isSelected) StudioCyan else StudioSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) StudioCyan else StudioBorder
                            ),
                            modifier = Modifier
                                .clickable { viewModel.setChordKey(keyName, pitch) }
                                .testTag("key_button_$keyName")
                        ) {
                            Text(
                                text = keyName,
                                color = if (isSelected) StudioDarkBg else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Scale chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        MusicalScale.MAJOR,
                        MusicalScale.NATURAL_MINOR,
                        MusicalScale.DORIAN,
                        MusicalScale.HARMONIC_MINOR,
                        MusicalScale.CHINESE_PENTATONIC
                    ).forEach { scale ->
                        val isSelected = state.chordScale == scale
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setChordScale(scale) },
                            label = { Text(scale.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioViolet,
                                selectedLabelColor = StudioDarkBg,
                                containerColor = StudioSurfaceElevated,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // Section 2: Genre & Mood Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.sectionGenre,
                    color = StudioViolet,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MusicGenre.entries.forEach { genre ->
                        val isSelected = state.chordGenre == genre
                        Surface(
                            color = if (isSelected) StudioViolet else StudioSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) StudioViolet else StudioBorder
                            ),
                            modifier = Modifier
                                .clickable { viewModel.setChordGenre(genre) }
                                .testTag("genre_chip_${genre.name}")
                        ) {
                            Text(
                                text = genre.displayName,
                                color = if (isSelected) StudioDarkBg else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Text(
                    text = strings.sectionMood,
                    color = StudioEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChordMood.entries.forEach { mood ->
                        val isSelected = state.chordMood == mood
                        Surface(
                            color = if (isSelected) StudioEmerald else StudioSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) StudioEmerald else StudioBorder
                            ),
                            modifier = Modifier
                                .clickable { viewModel.setChordMood(mood) }
                                .testTag("mood_chip_${mood.name}")
                        ) {
                            Text(
                                text = "${mood.emoji} ${mood.displayName}",
                                color = if (isSelected) StudioDarkBg else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Section 3: Harmonic Complexity & Voicing Pattern
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.sectionComplexityVoicing,
                    color = StudioAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                // Complexity row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChordComplexity.entries.forEach { comp ->
                        val isSelected = state.chordComplexity == comp
                        Surface(
                            color = if (isSelected) StudioAmber else StudioSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) StudioAmber else StudioBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setChordComplexity(comp) }
                                .testTag("complexity_${comp.name}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = comp.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) StudioDarkBg else TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Voicing styles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VoicingPattern.entries.forEach { pattern ->
                        val isSelected = state.chordVoicing == pattern
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setChordVoicing(pattern) },
                            label = { Text(pattern.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioCoral,
                                selectedLabelColor = StudioDarkBg,
                                containerColor = StudioSurfaceElevated,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // Section 4: Generated Progression & Audition Cards
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.sectionSequence,
                        color = StudioCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${state.activeChords.size} Chords",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Progression Template picker if multiple available
                if (state.suggestedTemplates.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.suggestedTemplates.forEach { tmpl ->
                            OutlinedButton(
                                onClick = { viewModel.selectProgressionTemplate(tmpl) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextPrimary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(tmpl.title, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Chord Visual Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.activeChords.forEach { chord ->
                        Surface(
                            color = StudioSurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier
                                .width(88.dp)
                                .clickable { viewModel.auditionChord(chord) }
                                .testTag("chord_card_${chord.name}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = chord.romanNumeral,
                                    fontSize = 11.sp,
                                    color = StudioViolet,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = chord.name,
                                    fontSize = 16.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = strings.audition,
                                        tint = StudioAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(strings.audition, fontSize = 9.sp, color = StudioAmber)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: Insert to Selected Track
            val activeTrack = state.tracks.firstOrNull { it.id == state.selectedTrackId }
            Button(
                onClick = {
                    viewModel.insertChordProgressionToTrack()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_chord_progression_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioCyan
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = StudioDarkBg)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${strings.applyChordsToTrack} (${activeTrack?.name ?: "Track"})",
                    color = StudioDarkBg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

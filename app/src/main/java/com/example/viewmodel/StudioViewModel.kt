package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.audio.InstrumentPatch
import com.example.audio.InstrumentType
import com.example.collab.CollaborationManager
import com.example.data.*
import com.example.midi.*
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import kotlin.math.floor

enum class StudioViewTab(val displayName: String) {
    ARRANGER("Arranger"),
    PIANO_ROLL("Piano Roll"),
    BEAT_SEQUENCER("Beat Sequencer"),
    CONTROLLER("Virtual Keys"),
    SOUND_LIBRARY("Sound Library"),
    COLLAB("Sync & Collab")
}

data class StudioUiState(
    val language: AppLanguage = AppLanguage.SIMPLIFIED_CHINESE,
    val currentProject: ProjectEntity? = null,
    val tracks: List<TrackEntity> = emptyList(),
    val clips: List<MidiClipEntity> = emptyList(),
    val collaborators: List<CollaboratorEntity> = emptyList(),
    val revisions: List<RevisionEntity> = emptyList(),
    val selectedTrackId: Long? = null,
    val selectedClipId: Long? = null,
    val currentTab: StudioViewTab = StudioViewTab.ARRANGER,
    val isPlaying: Boolean = false,
    val isRecording: Boolean = false,
    val currentBeat: Float = 0f,
    val masterVolume: Float = 0.85f,
    val isMetronomeOn: Boolean = false,
    val quantizeGrid: QuantizeGrid = QuantizeGrid.SIXTEENTH,
    // Chord progression generator state
    val chordRootKey: String = "C",
    val chordRootPitch: Int = 60,
    val chordScale: MusicalScale = MusicalScale.NATURAL_MINOR,
    val chordGenre: MusicGenre = MusicGenre.POP,
    val chordMood: ChordMood = ChordMood.UPLIFTING,
    val chordComplexity: ChordComplexity = ChordComplexity.EXTENDED,
    val chordVoicing: VoicingPattern = VoicingPattern.BLOCK_SUSTAIN,
    val suggestedTemplates: List<ProgressionTemplate> = emptyList(),
    val activeChords: List<GeneratedChord> = emptyList(),
    // Beat Sequencer state
    val currentBeatPattern: BeatPattern = BeatSequencerDefaults.getPresetPatterns()[0],
    val currentStepIndex: Int = 0,
    // Status message toast
    val statusMessage: String? = null
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StudioDatabase.getInstance(application)
    private val repository = StudioRepository(database.studioDao())
    val collaborationManager = CollaborationManager(repository)
    val audioEngine = AudioEngine()
    private val midiRecorder = MidiRecorder()

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null
    private var lastTriggeredBeat = -1f

    init {
        audioEngine.start()
        initializeProject()
        refreshChordSuggestions()
    }

    private fun initializeProject() {
        viewModelScope.launch {
            val project = repository.getInitialOrNewProject()

            // Observe project
            launch {
                repository.getProject(project.id).collect { proj ->
                    if (proj != null) {
                        _uiState.update { it.copy(currentProject = proj) }
                    }
                }
            }

            // Observe tracks
            launch {
                repository.getTracks(project.id).collect { tracksList ->
                    _uiState.update { state ->
                        val selectedId = if (state.selectedTrackId == null && tracksList.isNotEmpty()) {
                            tracksList.first().id
                        } else state.selectedTrackId
                        state.copy(tracks = tracksList, selectedTrackId = selectedId)
                    }
                }
            }

            // Observe clips
            launch {
                repository.getClips(project.id).collect { clipsList ->
                    _uiState.update { state ->
                        val selectedClip = if (state.selectedClipId == null && clipsList.isNotEmpty()) {
                            clipsList.first().id
                        } else state.selectedClipId
                        state.copy(clips = clipsList, selectedClipId = selectedClip)
                    }
                }
            }

            // Observe collaborators & revisions
            launch {
                repository.getCollaborators(project.id).collect { collabList ->
                    _uiState.update { it.copy(collaborators = collabList) }
                }
            }
            launch {
                repository.getRevisions(project.id).collect { revList ->
                    _uiState.update { it.copy(revisions = revList) }
                }
            }
        }
    }

    // --- Transport & Playback ---

    fun togglePlay() {
        if (_uiState.value.isPlaying) {
            stopPlayback()
        } else {
            startPlayback(recording = false)
        }
    }

    fun toggleRecord() {
        if (_uiState.value.isRecording) {
            stopPlayback()
        } else {
            startPlayback(recording = true)
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        if (_uiState.value.isRecording) {
            val recorded = midiRecorder.stopRecording()
            commitRecordedNotes(recorded)
        }
        _uiState.update { it.copy(isPlaying = false, isRecording = false, currentStepIndex = 0) }
    }

    fun seekToBeat(beat: Float) {
        val proj = _uiState.value.currentProject ?: return
        val clamped = beat.coerceIn(proj.loopStartBeat, proj.loopEndBeat)
        _uiState.update { it.copy(currentBeat = clamped) }
    }

    fun setTab(tab: StudioViewTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun selectTrack(trackId: Long) {
        _uiState.update { state ->
            val firstClipForTrack = state.clips.firstOrNull { it.trackId == trackId }?.id
            state.copy(selectedTrackId = trackId, selectedClipId = firstClipForTrack ?: state.selectedClipId)
        }
    }

    fun selectClip(clipId: Long) {
        val clip = _uiState.value.clips.firstOrNull { it.id == clipId } ?: return
        _uiState.update { it.copy(selectedClipId = clipId, selectedTrackId = clip.trackId) }
    }

    fun setBpm(newBpm: Int) {
        val proj = _uiState.value.currentProject ?: return
        viewModelScope.launch {
            repository.updateProject(proj.copy(bpm = newBpm.coerceIn(40, 240)))
        }
    }

    fun toggleLoop() {
        val proj = _uiState.value.currentProject ?: return
        viewModelScope.launch {
            repository.updateProject(proj.copy(isLooping = !proj.isLooping))
        }
    }

    fun toggleMetronome() {
        _uiState.update {
            val next = !it.isMetronomeOn
            audioEngine.isMetronomeEnabled = next
            it.copy(isMetronomeOn = next)
        }
    }

    fun setMasterVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        audioEngine.masterVolume = clamped
        _uiState.update { it.copy(masterVolume = clamped) }
    }

    fun setQuantizeGrid(grid: QuantizeGrid) {
        midiRecorder.quantizeGrid = grid
        _uiState.update { it.copy(quantizeGrid = grid) }
    }

    fun setLanguage(lang: AppLanguage) {
        _uiState.update { it.copy(language = lang) }
        showToast(if (lang == AppLanguage.SIMPLIFIED_CHINESE) "已切换至简体中文" else "Switched to English")
    }

    fun toggleLanguage() {
        val current = _uiState.value.language
        val next = if (current == AppLanguage.SIMPLIFIED_CHINESE) AppLanguage.ENGLISH else AppLanguage.SIMPLIFIED_CHINESE
        setLanguage(next)
    }

    // --- Audio Playback Loop ---

    private fun startPlayback(recording: Boolean) {
        val proj = _uiState.value.currentProject ?: return
        _uiState.update { it.copy(isPlaying = true, isRecording = recording) }

        if (recording) {
            midiRecorder.startRecording()
        }

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch(Dispatchers.Default) {
            val bpm = proj.bpm
            val beatIntervalMs = (60_000.0 / bpm).toFloat() // ms per beat
            val updateIntervalMs = 20L // 50 Hz clock update
            val beatsPerUpdate = updateIntervalMs / beatIntervalMs

            var beat = _uiState.value.currentBeat
            var prevIntegerBeat = floor(beat).toInt()
            var prevStepIdx = -1

            while (isActive) {
                val loopStart = proj.loopStartBeat
                val loopEnd = proj.loopEndBeat

                // Trigger notes in this time window
                triggerNotesForWindow(beat, beat + beatsPerUpdate)

                // Metronome click
                val currentIntBeat = floor(beat).toInt()
                if (currentIntBeat != prevIntegerBeat) {
                    prevIntegerBeat = currentIntBeat
                    if (_uiState.value.isMetronomeOn) {
                        val isDownbeat = (currentIntBeat % 4) == 0
                        audioEngine.triggerMetronome(isDownbeat)
                    }
                }

                // Update UI state & step sequencer playback
                val stepIdx = ((beat % 4.0f) * 4).toInt().coerceIn(0, 15)
                if (stepIdx != prevStepIdx) {
                    prevStepIdx = stepIdx
                    triggerSequencerStep(stepIdx, beatIntervalMs)
                }

                _uiState.update { it.copy(currentBeat = beat, currentStepIndex = stepIdx) }

                beat += beatsPerUpdate
                if (beat >= loopEnd) {
                    if (proj.isLooping) {
                        beat = loopStart
                    } else {
                        withContext(Dispatchers.Main) {
                            stopPlayback()
                        }
                        break
                    }
                }

                delay(updateIntervalMs)
            }
        }
    }

    private fun triggerNotesForWindow(fromBeat: Float, toBeat: Float) {
        val state = _uiState.value
        val tracksMap = state.tracks.associateBy { it.id }
        val anySolo = state.tracks.any { it.isSolo }

        for (clip in state.clips) {
            val track = tracksMap[clip.trackId] ?: continue
            if (track.isMuted) continue
            if (anySolo && !track.isSolo) continue

            val instType = InstrumentType.fromId(track.instrumentType)
            val notes = repository.deserializeNotes(clip.notesJson)

            for (note in notes) {
                val absoluteNoteStart = clip.startBeat + note.startBeat
                if (absoluteNoteStart in fromBeat..toBeat) {
                    audioEngine.noteOn(
                        midiNote = note.pitch,
                        velocity = note.velocity * track.volume,
                        instrument = instType,
                        trackId = track.id
                    )
                }
            }
        }
    }

    private fun commitRecordedNotes(recordedNotes: List<MidiNote>) {
        if (recordedNotes.isEmpty()) return
        val state = _uiState.value
        val trackId = state.selectedTrackId ?: state.tracks.firstOrNull()?.id ?: return
        val projId = state.currentProject?.id ?: return

        viewModelScope.launch {
            // Check if active clip exists on this track, or create one
            val existingClip = state.clips.firstOrNull { it.trackId == trackId }
            if (existingClip != null) {
                val currentNotes = repository.deserializeNotes(existingClip.notesJson).toMutableList()
                currentNotes.addAll(recordedNotes)
                repository.saveClipNotes(
                    clipId = existingClip.id,
                    trackId = trackId,
                    projectId = projId,
                    name = existingClip.name,
                    startBeat = existingClip.startBeat,
                    durationBeats = (existingClip.durationBeats).coerceAtLeast(16f),
                    notes = currentNotes
                )
            } else {
                repository.saveClipNotes(
                    clipId = 0L,
                    trackId = trackId,
                    projectId = projId,
                    name = "Recorded MIDI Take",
                    startBeat = 0f,
                    durationBeats = 16f,
                    notes = recordedNotes
                )
            }
            showToast("MIDI recording saved (${recordedNotes.size} notes)")
        }
    }

    // --- Real-Time Controller & Live Input ---

    fun onLiveNoteDown(pitch: Int, velocity: Float = 0.9f) {
        val state = _uiState.value
        val track = state.tracks.firstOrNull { it.id == state.selectedTrackId }
            ?: state.tracks.firstOrNull()
        val inst = if (track != null) InstrumentType.fromId(track.instrumentType) else InstrumentType.GRAND_PIANO
        audioEngine.noteOn(pitch, velocity, inst, track?.id ?: 0L)

        if (state.isRecording) {
            midiRecorder.onNoteOn(pitch, state.currentBeat, velocity)
        }
    }

    fun onLiveNoteUp(pitch: Int) {
        val state = _uiState.value
        audioEngine.noteOff(pitch, state.selectedTrackId ?: 0L)
        if (state.isRecording) {
            midiRecorder.onNoteOff(pitch, state.currentBeat)
        }
    }

    fun onLiveDrumTrigger(drumNote: Int, velocity: Float = 0.95f) {
        val state = _uiState.value
        audioEngine.playDrum(drumNote, velocity)
        if (state.isRecording) {
            midiRecorder.onNoteOn(drumNote, state.currentBeat, velocity)
            viewModelScope.launch {
                delay(100)
                midiRecorder.onNoteOff(drumNote, _uiState.value.currentBeat)
            }
        }
    }

    // --- Smart Chord Progression Generator ---

    fun setChordKey(keyName: String, pitch: Int) {
        _uiState.update { it.copy(chordRootKey = keyName, chordRootPitch = pitch) }
        refreshChordSuggestions()
    }

    fun setChordScale(scale: MusicalScale) {
        _uiState.update { it.copy(chordScale = scale) }
        refreshChordSuggestions()
    }

    fun setChordGenre(genre: MusicGenre) {
        _uiState.update { it.copy(chordGenre = genre) }
        refreshChordSuggestions()
    }

    fun setChordMood(mood: ChordMood) {
        _uiState.update { it.copy(chordMood = mood) }
        refreshChordSuggestions()
    }

    fun setChordComplexity(complexity: ChordComplexity) {
        _uiState.update { it.copy(chordComplexity = complexity) }
        refreshChordSuggestions()
    }

    fun setChordVoicing(voicing: VoicingPattern) {
        _uiState.update { it.copy(chordVoicing = voicing) }
    }

    private fun refreshChordSuggestions() {
        val state = _uiState.value
        val suggestions = ChordProgressionGenerator.getSuggestedProgressions(state.chordGenre, state.chordMood)
        val firstTemplate = suggestions.firstOrNull()
        val chords = if (firstTemplate != null) {
            ChordProgressionGenerator.buildChords(
                template = firstTemplate,
                rootPitch = state.chordRootPitch,
                scale = state.chordScale,
                complexity = state.chordComplexity
            )
        } else emptyList()

        _uiState.update {
            it.copy(suggestedTemplates = suggestions, activeChords = chords)
        }
    }

    fun selectProgressionTemplate(template: ProgressionTemplate) {
        val state = _uiState.value
        val chords = ChordProgressionGenerator.buildChords(
            template = template,
            rootPitch = state.chordRootPitch,
            scale = state.chordScale,
            complexity = state.chordComplexity
        )
        _uiState.update { it.copy(activeChords = chords) }
    }

    fun auditionChord(chord: GeneratedChord) {
        val state = _uiState.value
        val track = state.tracks.firstOrNull { it.id == state.selectedTrackId }
        val inst = if (track != null) InstrumentType.fromId(track.instrumentType) else InstrumentType.GRAND_PIANO

        viewModelScope.launch {
            for (pitch in chord.pitches) {
                audioEngine.noteOn(pitch, 0.82f, inst, 0L)
            }
            audioEngine.noteOn(chord.rootPitch - 12, 0.9f, inst, 0L) // Bass root
            delay(800)
            for (pitch in chord.pitches) {
                audioEngine.noteOff(pitch, 0L)
            }
            audioEngine.noteOff(chord.rootPitch - 12, 0L)
        }
    }

    fun insertChordProgressionToTrack() {
        val state = _uiState.value
        val trackId = state.selectedTrackId ?: state.tracks.firstOrNull()?.id ?: return
        val projId = state.currentProject?.id ?: return
        val chords = state.activeChords
        if (chords.isEmpty()) return

        val midiNotes = ChordProgressionGenerator.convertToMidiNotes(
            chords = chords,
            startBeat = 0f,
            pattern = state.chordVoicing
        )

        viewModelScope.launch {
            val existing = state.clips.firstOrNull { it.trackId == trackId }
            val clipId = existing?.id ?: 0L
            val clipName = "${state.chordRootKey} ${state.chordGenre.displayName} Chords"
            repository.saveClipNotes(
                clipId = clipId,
                trackId = trackId,
                projectId = projId,
                name = clipName,
                startBeat = 0f,
                durationBeats = 16f,
                notes = midiNotes
            )
            showToast("Inserted chords into track ($clipName)")
        }
    }

    // --- Advanced Step Sequencer & Synth Matrix ---

    private fun triggerSequencerStep(stepIdx: Int, beatIntervalMs: Float) {
        val pattern = _uiState.value.currentBeatPattern
        val anySolo = pattern.lanes.any { it.isSolo }
        val stepDurationMs = (beatIntervalMs / 4.0f * 0.85f).toLong().coerceIn(35L, 450L)

        for (lane in pattern.lanes) {
            if (lane.isMuted) continue
            if (anySolo && !lane.isSolo) continue
            val step = lane.steps.getOrNull(stepIdx) ?: continue
            if (!step.active) continue

            if (lane.instrumentType == InstrumentType.DRUM_KIT) {
                audioEngine.playDrum(lane.midiNote, step.velocity)
            } else {
                val trackId = lane.id.hashCode().toLong()
                audioEngine.noteOn(lane.midiNote, step.velocity, lane.instrumentType, trackId)
                viewModelScope.launch {
                    delay(stepDurationMs)
                    audioEngine.noteOff(lane.midiNote, trackId)
                }
            }
        }
    }

    fun toggleStep(laneId: String, stepIndex: Int) {
        var toggledOn = false
        var lanePitch = 60
        var laneInst = InstrumentType.DRUM_KIT

        _uiState.update { state ->
            val updatedLanes = state.currentBeatPattern.lanes.map { lane ->
                if (lane.id == laneId) {
                    lanePitch = lane.midiNote
                    laneInst = lane.instrumentType
                    val updatedSteps = lane.steps.mapIndexed { idx, step ->
                        if (idx == stepIndex) {
                            val newActive = !step.active
                            if (newActive) toggledOn = true
                            step.copy(active = newActive)
                        } else step
                    }
                    lane.copy(steps = updatedSteps)
                } else lane
            }
            val updatedPattern = state.currentBeatPattern.copy(lanes = updatedLanes)
            state.copy(currentBeatPattern = updatedPattern)
        }

        // Audition note when toggling ON
        if (toggledOn) {
            if (laneInst == InstrumentType.DRUM_KIT) {
                audioEngine.playDrum(lanePitch, 0.9f)
            } else {
                audioEngine.noteOn(lanePitch, 0.85f, laneInst, laneId.hashCode().toLong())
                viewModelScope.launch {
                    delay(200L)
                    audioEngine.noteOff(lanePitch, laneId.hashCode().toLong())
                }
            }
        }
    }

    fun auditionLane(laneId: String) {
        val lane = _uiState.value.currentBeatPattern.lanes.firstOrNull { it.id == laneId } ?: return
        if (lane.instrumentType == InstrumentType.DRUM_KIT) {
            audioEngine.playDrum(lane.midiNote, 0.95f)
        } else {
            audioEngine.noteOn(lane.midiNote, 0.9f, lane.instrumentType, lane.id.hashCode().toLong())
            viewModelScope.launch {
                delay(260L)
                audioEngine.noteOff(lane.midiNote, lane.id.hashCode().toLong())
            }
        }
    }

    fun setLanePitch(laneId: String, newMidiNote: Int, newNoteName: String) {
        _uiState.update { state ->
            val updatedLanes = state.currentBeatPattern.lanes.map { lane ->
                if (lane.id == laneId) {
                    lane.copy(midiNote = newMidiNote, noteName = newNoteName)
                } else lane
            }
            state.copy(currentBeatPattern = state.currentBeatPattern.copy(lanes = updatedLanes))
        }
        auditionLane(laneId)
    }

    fun toggleLaneMute(laneId: String) {
        _uiState.update { state ->
            val updatedLanes = state.currentBeatPattern.lanes.map { lane ->
                if (lane.id == laneId) lane.copy(isMuted = !lane.isMuted) else lane
            }
            state.copy(currentBeatPattern = state.currentBeatPattern.copy(lanes = updatedLanes))
        }
    }

    fun toggleLaneSolo(laneId: String) {
        _uiState.update { state ->
            val updatedLanes = state.currentBeatPattern.lanes.map { lane ->
                if (lane.id == laneId) lane.copy(isSolo = !lane.isSolo) else lane
            }
            state.copy(currentBeatPattern = state.currentBeatPattern.copy(lanes = updatedLanes))
        }
    }

    fun addSynthLane(instrumentType: InstrumentType, pitch: Int, name: String, colorHex: Long) {
        val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val oct = (pitch / 12) - 1
        val noteStr = "${noteNames[(pitch % 12 + 12) % 12]}$oct"

        _uiState.update { state ->
            val newLane = com.example.midi.DrumLane(
                id = "synth_${System.currentTimeMillis()}",
                name = name,
                midiNote = pitch,
                noteName = noteStr,
                color = androidx.compose.ui.graphics.Color(colorHex),
                instrumentType = instrumentType,
                steps = List(state.currentBeatPattern.stepCount) { com.example.midi.DrumStep(active = false) }
            )
            val updatedLanes = state.currentBeatPattern.lanes + newLane
            state.copy(currentBeatPattern = state.currentBeatPattern.copy(lanes = updatedLanes))
        }
        showToast("Added synth track: $name ($noteStr)")
    }

    fun removeLane(laneId: String) {
        _uiState.update { state ->
            val updatedLanes = state.currentBeatPattern.lanes.filterNot { it.id == laneId }
            state.copy(currentBeatPattern = state.currentBeatPattern.copy(lanes = updatedLanes))
        }
    }

    fun setStepVelocity(laneId: String, stepIndex: Int, velocity: Float) {
        _uiState.update { state ->
            val updatedLanes = state.currentBeatPattern.lanes.map { lane ->
                if (lane.id == laneId) {
                    val updatedSteps = lane.steps.mapIndexed { idx, step ->
                        if (idx == stepIndex) step.copy(velocity = velocity.coerceIn(0.1f, 1f)) else step
                    }
                    lane.copy(steps = updatedSteps)
                } else lane
            }
            state.copy(currentBeatPattern = state.currentBeatPattern.copy(lanes = updatedLanes))
        }
    }

    fun setSequencerSwing(swing: Int) {
        _uiState.update { state ->
            state.copy(currentBeatPattern = state.currentBeatPattern.copy(swingPercent = swing.coerceIn(0, 75)))
        }
    }

    fun loadPresetBeat(preset: BeatPattern) {
        _uiState.update { it.copy(currentBeatPattern = preset) }
        showToast("Loaded preset: ${preset.name}")
    }

    fun clearSequencer() {
        _uiState.update { state ->
            val emptyLanes = BeatSequencerDefaults.createEmptyLanes(state.currentBeatPattern.stepCount)
            state.copy(currentBeatPattern = state.currentBeatPattern.copy(lanes = emptyLanes))
        }
        showToast("Sequencer grid cleared")
    }

    fun insertBeatPatternToTimeline() {
        val state = _uiState.value
        val projId = state.currentProject?.id ?: return

        viewModelScope.launch {
            // 1. Separate drums and synths
            val drumNotes = BeatSequencerDefaults.patternToMidiNotes(
                pattern = state.currentBeatPattern,
                startBeat = 0f,
                totalBars = 4,
                onlyDrums = true
            )

            if (drumNotes.isNotEmpty()) {
                var drumTrack = state.tracks.firstOrNull {
                    val inst = InstrumentType.fromId(it.instrumentType)
                    inst == InstrumentType.DRUM_KIT || inst.isPercussion
                }
                val drumTrackId = drumTrack?.id ?: repository.addTrack(
                    projectId = projId,
                    name = "Drum Machine",
                    instrumentType = InstrumentType.DRUM_KIT,
                    colorHex = 0xFFEF4444
                )
                val existingClip = state.clips.firstOrNull { it.trackId == drumTrackId }
                repository.saveClipNotes(
                    clipId = existingClip?.id ?: 0L,
                    trackId = drumTrackId,
                    projectId = projId,
                    name = "${state.currentBeatPattern.name} (Drums)",
                    startBeat = 0f,
                    durationBeats = 16f,
                    notes = drumNotes
                )
            }

            // 2. Commit each synth track that has active steps
            val synthLanes = state.currentBeatPattern.lanes.filter { it.isSynth && it.steps.any { s -> s.active } }
            for (lane in synthLanes) {
                val singleLanePattern = state.currentBeatPattern.copy(lanes = listOf(lane))
                val synthNotes = BeatSequencerDefaults.patternToMidiNotes(
                    pattern = singleLanePattern,
                    startBeat = 0f,
                    totalBars = 4,
                    onlySynths = true
                )
                if (synthNotes.isNotEmpty()) {
                    var synthTrack = state.tracks.firstOrNull {
                        val inst = InstrumentType.fromId(it.instrumentType)
                        inst == lane.instrumentType
                    }
                    val targetTrackId = synthTrack?.id ?: repository.addTrack(
                        projectId = projId,
                        name = lane.name,
                        instrumentType = lane.instrumentType,
                        colorHex = lane.color.value.toLong()
                    )
                    val existingClip = state.clips.firstOrNull { it.trackId == targetTrackId }
                    repository.saveClipNotes(
                        clipId = existingClip?.id ?: 0L,
                        trackId = targetTrackId,
                        projectId = projId,
                        name = "${lane.name} (Rhythm)",
                        startBeat = 0f,
                        durationBeats = 16f,
                        notes = synthNotes
                    )
                }
            }

            showToast("Pattern applied to timeline (Drums & Synth Tracks)")
        }
    }

    // --- Track Management ---

    fun addNewTrack(name: String, instrument: InstrumentType) {
        val projId = _uiState.value.currentProject?.id ?: return
        viewModelScope.launch {
            val trackId = repository.addTrack(
                projectId = projId,
                name = name,
                instrumentType = instrument,
                colorHex = instrument.defaultColor.value.toLong()
            )
            // Automatically add an initial empty 16-beat clip so the track is immediately visible with clips on the timeline
            repository.saveClipNotes(
                clipId = 0L,
                trackId = trackId,
                projectId = projId,
                name = "$name Pattern 1",
                startBeat = 0f,
                durationBeats = 16f,
                notes = emptyList()
            )
            selectTrack(trackId)
            showToast("已创建并添加 $name 音轨")
        }
    }

    fun addNewTrackWithNotes(name: String, instrument: InstrumentType, notes: List<MidiNote>) {
        val projId = _uiState.value.currentProject?.id ?: return
        viewModelScope.launch {
            val trackId = repository.addTrack(
                projectId = projId,
                name = name,
                instrumentType = instrument,
                colorHex = instrument.defaultColor.value.toLong()
            )
            val durationBeats = (notes.maxOfOrNull { it.startBeat + it.durationBeats } ?: 16f).coerceAtLeast(16f)
            repository.saveClipNotes(
                clipId = 0L,
                trackId = trackId,
                projectId = projId,
                name = "$name Take",
                startBeat = 0f,
                durationBeats = durationBeats,
                notes = notes
            )
            selectTrack(trackId)
            showToast("已将 $name (${notes.size} 音符) 添加至时间轴")
        }
    }

    fun createAudioRecordingTrack(name: String, audioFile: java.io.File, durationSeconds: Float) {
        val projId = _uiState.value.currentProject?.id ?: return
        val bpm = _uiState.value.currentProject?.bpm ?: 120
        val durationBeats = (durationSeconds / 60.0f * bpm).coerceAtLeast(4f)

        viewModelScope.launch {
            val trackId = repository.addTrack(
                projectId = projId,
                name = name,
                instrumentType = InstrumentType.FLUTE, // Audio vocal/mic instrument representation
                colorHex = 0xFFEF4444
            )
            // Create a waveform representation clip
            val noteCount = (durationBeats * 2).toInt().coerceAtLeast(4)
            val sampleNotes = List(noteCount) { i ->
                MidiNote(
                    pitch = 60 + (i % 5),
                    startBeat = i * 0.5f,
                    durationBeats = 0.5f,
                    velocity = 0.8f
                )
            }
            repository.saveClipNotes(
                clipId = 0L,
                trackId = trackId,
                projectId = projId,
                name = "${audioFile.nameWithoutExtension} (Audio)",
                startBeat = 0f,
                durationBeats = durationBeats,
                notes = sampleNotes
            )
            selectTrack(trackId)
            showToast("录音已创建为新音轨: $name")
        }
    }

    fun updateTrackVolume(trackId: Long, vol: Float) {
        val track = _uiState.value.tracks.firstOrNull { it.id == trackId } ?: return
        viewModelScope.launch {
            repository.updateTrack(track.copy(volume = vol.coerceIn(0f, 1f)))
        }
    }

    fun updateTrackPan(trackId: Long, pan: Float) {
        val track = _uiState.value.tracks.firstOrNull { it.id == trackId } ?: return
        viewModelScope.launch {
            repository.updateTrack(track.copy(pan = pan.coerceIn(-1f, 1f)))
        }
    }

    fun toggleTrackMute(trackId: Long) {
        val track = _uiState.value.tracks.firstOrNull { it.id == trackId } ?: return
        viewModelScope.launch {
            repository.updateTrack(track.copy(isMuted = !track.isMuted))
        }
    }

    fun toggleTrackSolo(trackId: Long) {
        val track = _uiState.value.tracks.firstOrNull { it.id == trackId } ?: return
        viewModelScope.launch {
            repository.updateTrack(track.copy(isSolo = !track.isSolo))
        }
    }

    fun deleteTrack(trackId: Long) {
        val track = _uiState.value.tracks.firstOrNull { it.id == trackId } ?: return
        viewModelScope.launch {
            repository.deleteTrack(track)
            showToast("Deleted ${track.name}")
        }
    }

    // --- Piano Roll Note Editing ---

    fun addNoteToActiveClip(pitch: Int, startBeat: Float, durationBeats: Float = 1.0f) {
        val state = _uiState.value
        val clipId = state.selectedClipId ?: return
        val clip = state.clips.firstOrNull { it.id == clipId } ?: return

        val currentNotes = repository.deserializeNotes(clip.notesJson).toMutableList()
        currentNotes.add(
            MidiNote(
                pitch = pitch,
                startBeat = startBeat,
                durationBeats = durationBeats,
                velocity = 0.85f
            )
        )

        viewModelScope.launch {
            repository.saveClipNotes(
                clipId = clip.id,
                trackId = clip.trackId,
                projectId = clip.projectId,
                name = clip.name,
                startBeat = clip.startBeat,
                durationBeats = clip.durationBeats,
                notes = currentNotes
            )
        }
    }

    fun deleteNoteFromActiveClip(noteId: String) {
        val state = _uiState.value
        val clipId = state.selectedClipId ?: return
        val clip = state.clips.firstOrNull { it.id == clipId } ?: return

        val currentNotes = repository.deserializeNotes(clip.notesJson).filter { it.id != noteId }
        viewModelScope.launch {
            repository.saveClipNotes(
                clipId = clip.id,
                trackId = clip.trackId,
                projectId = clip.projectId,
                name = clip.name,
                startBeat = clip.startBeat,
                durationBeats = clip.durationBeats,
                notes = currentNotes
            )
        }
    }

    // --- Sound Library & Patch Editing ---

    fun updateInstrumentPatch(patch: InstrumentPatch) {
        audioEngine.updatePatch(patch)
        showToast("Updated ${patch.instrumentType.displayName} timbre parameters")
    }

    fun assignInstrumentToTrack(trackId: Long, instrument: InstrumentType) {
        val track = _uiState.value.tracks.firstOrNull { it.id == trackId } ?: return
        viewModelScope.launch {
            repository.updateTrack(
                track.copy(
                    instrumentType = instrument.id,
                    colorHex = instrument.defaultColor.value.toLong()
                )
            )
            showToast("Assigned ${instrument.displayName} to track ${track.name}")
        }
    }

    fun auditionInstrument(instrument: InstrumentType) {
        audioEngine.noteOn(60, 0.85f, instrument, 0L)
        audioEngine.noteOn(64, 0.8f, instrument, 0L)
        audioEngine.noteOn(67, 0.8f, instrument, 0L)
        viewModelScope.launch {
            delay(500)
            audioEngine.noteOff(60, 0L)
            audioEngine.noteOff(64, 0L)
            audioEngine.noteOff(67, 0L)
        }
    }

    // --- Sync & Collaboration ---

    fun syncProjectNow() {
        val proj = _uiState.value.currentProject ?: return
        viewModelScope.launch {
            repository.commitRevision(
                projectId = proj.id,
                author = "You",
                message = "Synced changes across all linked devices"
            )
            showToast("Project synced! Code: ${proj.shareCode}")
        }
    }

    fun exportProjectJson(): String {
        val state = _uiState.value
        val proj = state.currentProject ?: return ""
        val clipsMap = state.clips.groupBy { it.trackId }

        val tracksData = state.tracks.map { track ->
            val trackClips = clipsMap[track.id] ?: emptyList()
            val allNotes = trackClips.flatMap { repository.deserializeNotes(it.notesJson) }
            StandardMidiHelper.TrackExportData(
                trackName = track.name,
                instrument = InstrumentType.fromId(track.instrumentType),
                notes = allNotes
            )
        }

        return StandardMidiHelper.exportProjectToJson(
            title = proj.title,
            bpm = proj.bpm,
            timeSignature = proj.timeSignature,
            shareCode = proj.shareCode,
            tracks = tracksData
        )
    }

    fun importProjectBundle(jsonString: String) {
        val bundle = StandardMidiHelper.parseProjectFromJson(jsonString)
        if (bundle == null) {
            showToast("Failed to parse project JSON")
            return
        }
        viewModelScope.launch {
            repository.importProjectBundle(bundle)
            showToast("Successfully imported project: ${bundle.title}")
        }
    }

    fun exportMidiFile(context: Context): File? {
        val state = _uiState.value
        val proj = state.currentProject ?: return null
        val clipsMap = state.clips.groupBy { it.trackId }

        val tracksData = state.tracks.map { track ->
            val trackClips = clipsMap[track.id] ?: emptyList()
            val allNotes = trackClips.flatMap { repository.deserializeNotes(it.notesJson) }
            StandardMidiHelper.TrackExportData(
                trackName = track.name,
                instrument = InstrumentType.fromId(track.instrumentType),
                notes = allNotes
            )
        }

        val bytes = StandardMidiHelper.createStandardMidiFile(proj.bpm, tracksData)
        val file = File(context.cacheDir, "${proj.title.replace(" ", "_")}.mid")
        file.writeBytes(bytes)
        showToast("MIDI file exported: ${file.name}")
        return file
    }

    fun exportWavFile(context: Context): File? {
        val state = _uiState.value
        val proj = state.currentProject ?: return null
        val tracksMap = state.tracks.associateBy { it.id }

        val exportNotes = mutableListOf<AudioEngine.ExportNoteEvent>()
        for (clip in state.clips) {
            val track = tracksMap[clip.trackId] ?: continue
            if (track.isMuted) continue
            val inst = InstrumentType.fromId(track.instrumentType)
            val notes = repository.deserializeNotes(clip.notesJson)
            for (n in notes) {
                exportNotes.add(
                    AudioEngine.ExportNoteEvent(
                        midiNote = n.pitch,
                        startBeat = clip.startBeat + n.startBeat,
                        durationBeats = n.durationBeats,
                        velocity = n.velocity,
                        instrument = inst,
                        trackVolume = track.volume
                    )
                )
            }
        }

        val totalBeats = proj.loopEndBeat
        val totalSecs = (totalBeats * 60f / proj.bpm) + 2.0f // +2s tail
        val wavFile = File(context.cacheDir, "${proj.title.replace(" ", "_")}.wav")

        val success = audioEngine.exportWav(wavFile, totalSecs, proj.bpm) {
            exportNotes
        }

        if (success) {
            showToast("Mixdown WAV rendered: ${wavFile.name} (${wavFile.length() / 1024} KB)")
            return wavFile
        } else {
            showToast("WAV render failed")
            return null
        }
    }

    fun showToast(msg: String) {
        _uiState.update { it.copy(statusMessage = msg) }
    }

    fun clearToast() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
        audioEngine.stop()
    }
}

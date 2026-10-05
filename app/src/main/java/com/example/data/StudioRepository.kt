package com.example.data

import com.example.audio.InstrumentType
import com.example.midi.BeatPattern
import com.example.midi.BeatSequencerDefaults
import com.example.midi.MidiNote
import com.example.midi.StandardMidiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class StudioRepository(private val dao: StudioDao) {

    val allProjects: Flow<List<ProjectEntity>> = dao.getAllProjects()

    fun getProject(id: Long): Flow<ProjectEntity?> = dao.getProjectById(id)

    fun getTracks(projectId: Long): Flow<List<TrackEntity>> = dao.getTracksForProject(projectId)

    fun getClips(projectId: Long): Flow<List<MidiClipEntity>> = dao.getClipsForProject(projectId)

    fun getCollaborators(projectId: Long): Flow<List<CollaboratorEntity>> = dao.getCollaborators(projectId)

    fun getRevisions(projectId: Long): Flow<List<RevisionEntity>> = dao.getRevisions(projectId)

    suspend fun getInitialOrNewProject(): ProjectEntity = withContext(Dispatchers.IO) {
        val existing = dao.getLatestProjectDirect()
        if (existing != null) {
            return@withContext existing
        }
        return@withContext seedDemoProject()
    }

    suspend fun createNewProject(title: String, bpm: Int = 120): Long = withContext(Dispatchers.IO) {
        val proj = ProjectEntity(
            title = title,
            bpm = bpm,
            timeSignature = "4/4",
            loopStartBeat = 0f,
            loopEndBeat = 16f,
            shareCode = "HRMN-" + (1000..9999).random(),
            cloudSyncStatus = "Synced",
            syncRevision = 1
        )
        val projId = dao.insertProject(proj)

        // Add 3 default starter tracks
        dao.insertTrack(
            TrackEntity(
                projectId = projId,
                name = "Grand Piano",
                instrumentType = InstrumentType.GRAND_PIANO.id,
                trackOrder = 0,
                colorHex = 0xFF38BDF8
            )
        )
        dao.insertTrack(
            TrackEntity(
                projectId = projId,
                name = "Drum Machine",
                instrumentType = InstrumentType.DRUM_KIT.id,
                trackOrder = 1,
                colorHex = 0xFFEF4444
            )
        )
        dao.insertTrack(
            TrackEntity(
                projectId = projId,
                name = "Slap Bass",
                instrumentType = InstrumentType.SLAP_BASS.id,
                trackOrder = 2,
                colorHex = 0xFF10B981
            )
        )

        dao.insertCollaborator(
            CollaboratorEntity(
                projectId = projId,
                name = "You (Producer)",
                deviceName = "Android DAW",
                avatarColorHex = 0xFF38BDF8,
                currentActivity = "Arranging Master Track"
            )
        )

        dao.insertRevision(
            RevisionEntity(
                projectId = projId,
                revisionNumber = 1,
                author = "You",
                message = "Project initialized: $title"
            )
        )

        projId
    }

    suspend fun addTrack(
        projectId: Long,
        name: String,
        instrumentType: InstrumentType,
        colorHex: Long
    ): Long = withContext(Dispatchers.IO) {
        val tracks = dao.getTracksForProjectDirect(projectId)
        val track = TrackEntity(
            projectId = projectId,
            name = name,
            instrumentType = instrumentType.id,
            colorHex = colorHex,
            trackOrder = tracks.size
        )
        dao.insertTrack(track)
    }

    suspend fun updateTrack(track: TrackEntity) = withContext(Dispatchers.IO) {
        dao.updateTrack(track)
    }

    suspend fun deleteTrack(track: TrackEntity) = withContext(Dispatchers.IO) {
        dao.deleteTrack(track)
    }

    suspend fun saveClipNotes(clipId: Long, trackId: Long, projectId: Long, name: String, startBeat: Float, durationBeats: Float, notes: List<MidiNote>) = withContext(Dispatchers.IO) {
        val notesJson = serializeNotes(notes)
        val clip = MidiClipEntity(
            id = clipId,
            trackId = trackId,
            projectId = projectId,
            name = name,
            startBeat = startBeat,
            durationBeats = durationBeats,
            notesJson = notesJson
        )
        dao.insertClip(clip)
    }

    suspend fun addOrUpdateClip(clip: MidiClipEntity) = withContext(Dispatchers.IO) {
        dao.insertClip(clip)
    }

    suspend fun deleteClip(clip: MidiClipEntity) = withContext(Dispatchers.IO) {
        dao.deleteClip(clip)
    }

    suspend fun updateProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        dao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun commitRevision(projectId: Long, author: String, message: String) = withContext(Dispatchers.IO) {
        val current = dao.getLatestProjectDirect()
        val nextRev = (current?.syncRevision ?: 0) + 1
        if (current != null) {
            dao.updateProject(current.copy(syncRevision = nextRev, cloudSyncStatus = "Synced"))
        }
        dao.insertRevision(
            RevisionEntity(
                projectId = projectId,
                revisionNumber = nextRev,
                author = author,
                message = message
            )
        )
    }

    suspend fun importProjectBundle(bundle: StandardMidiHelper.ParsedProjectBundle): Long = withContext(Dispatchers.IO) {
        val project = ProjectEntity(
            title = bundle.title,
            bpm = bundle.bpm,
            timeSignature = bundle.timeSignature,
            shareCode = bundle.shareCode,
            loopStartBeat = 0f,
            loopEndBeat = 16f,
            cloudSyncStatus = "Synced",
            syncRevision = 1
        )
        val projId = dao.insertProject(project)

        for ((idx, t) in bundle.tracks.withIndex()) {
            val trackId = dao.insertTrack(
                TrackEntity(
                    projectId = projId,
                    name = t.trackName,
                    instrumentType = t.instrument.id,
                    trackOrder = idx,
                    colorHex = t.instrument.defaultColor.value.toLong()
                )
            )

            if (t.notes.isNotEmpty()) {
                val clip = MidiClipEntity(
                    trackId = trackId,
                    projectId = projId,
                    name = "${t.trackName} Clip",
                    startBeat = 0f,
                    durationBeats = 16f,
                    notesJson = serializeNotes(t.notes)
                )
                dao.insertClip(clip)
            }
        }

        dao.insertCollaborator(
            CollaboratorEntity(
                projectId = projId,
                name = "Cross-Platform Sync",
                deviceName = "Cloud Workspace",
                avatarColorHex = 0xFF818CF8,
                currentActivity = "Imported remote bundle"
            )
        )

        dao.insertRevision(
            RevisionEntity(
                projectId = projId,
                revisionNumber = 1,
                author = "Cross-Platform Sync",
                message = "Synced project from external device"
            )
        )

        projId
    }

    fun serializeNotes(notes: List<MidiNote>): String {
        val arr = JSONArray()
        for (n in notes) {
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("pitch", n.pitch)
            obj.put("start", n.startBeat.toDouble())
            obj.put("dur", n.durationBeats.toDouble())
            obj.put("vel", n.velocity.toDouble())
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeNotes(json: String?): List<MidiNote> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<MidiNote>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    MidiNote(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        pitch = obj.getInt("pitch"),
                        startBeat = obj.getDouble("start").toFloat(),
                        durationBeats = obj.getDouble("dur").toFloat(),
                        velocity = obj.optDouble("vel", 0.85).toFloat()
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private suspend fun seedDemoProject(): ProjectEntity {
        val project = ProjectEntity(
            title = "Neon Horizons (Synthwave & Silk)",
            bpm = 124,
            timeSignature = "4/4",
            loopStartBeat = 0f,
            loopEndBeat = 16f,
            isLooping = true,
            shareCode = "HRMN-9042",
            cloudSyncStatus = "Synced",
            syncRevision = 3
        )
        val projId = dao.insertProject(project)

        // 1. Drums Track (Full 16-bar groove)
        val drumTrackId = dao.insertTrack(
            TrackEntity(
                projectId = projId,
                name = "Drum Machine",
                instrumentType = InstrumentType.DRUM_KIT.id,
                volume = 0.9f,
                colorHex = 0xFFEF4444,
                trackOrder = 0
            )
        )
        val drumPattern = BeatSequencerDefaults.getPresetPatterns()[0] // House 4-on-floor
        val drumNotes = BeatSequencerDefaults.patternToMidiNotes(drumPattern, 0f, 4, onlyDrums = true) // 4 bars = 16 beats
        dao.insertClip(
            MidiClipEntity(
                trackId = drumTrackId,
                projectId = projId,
                name = "Punchy Studio Beat",
                startBeat = 0f,
                durationBeats = 16f,
                notesJson = serializeNotes(drumNotes)
            )
        )

        // 2. Bass Track (Slap/Sub bass groove in A Minor)
        val bassTrackId = dao.insertTrack(
            TrackEntity(
                projectId = projId,
                name = "Slap Bassline",
                instrumentType = InstrumentType.SLAP_BASS.id,
                volume = 0.88f,
                colorHex = 0xFF10B981,
                trackOrder = 1
            )
        )
        // A Minor bassline: A1(33), C2(36), D2(38), F1(29), G1(31)
        val bassNotes = mutableListOf<MidiNote>()
        val bassRhythm = listOf(
            Triple(33, 0f, 0.75f),
            Triple(33, 1.0f, 0.5f),
            Triple(36, 1.75f, 0.5f),
            Triple(38, 2.5f, 0.75f),
            Triple(29, 4f, 0.75f),
            Triple(29, 5f, 0.5f),
            Triple(31, 6f, 0.75f),
            Triple(33, 7.5f, 0.5f)
        )
        // Loop over 2 repetitions for 16 beats
        for (rep in 0..1) {
            val offset = rep * 8f
            for (b in bassRhythm) {
                bassNotes.add(MidiNote(pitch = b.first, startBeat = b.second + offset, durationBeats = b.third, velocity = 0.9f))
            }
        }
        dao.insertClip(
            MidiClipEntity(
                trackId = bassTrackId,
                projectId = projId,
                name = "Funky Drive Bass",
                startBeat = 0f,
                durationBeats = 16f,
                notesJson = serializeNotes(bassNotes)
            )
        )

        // 3. Electric Piano Track (Rhodes Lush Chords: Am7, Fmaj7, Cmaj7, G7)
        val epTrackId = dao.insertTrack(
            TrackEntity(
                projectId = projId,
                name = "Rhodes E-Piano",
                instrumentType = InstrumentType.ELECTRIC_PIANO.id,
                volume = 0.85f,
                colorHex = 0xFF38BDF8,
                trackOrder = 2
            )
        )
        val epNotes = mutableListOf<MidiNote>()
        // Bar 1-2: Am7 (A3, C4, E4, G4)
        for (p in listOf(57, 60, 64, 67)) {
            epNotes.add(MidiNote(pitch = p, startBeat = 0f, durationBeats = 3.8f, velocity = 0.8f))
            epNotes.add(MidiNote(pitch = p, startBeat = 8f, durationBeats = 3.8f, velocity = 0.8f))
        }
        // Bar 2-3: Fmaj7 (F3, A3, C4, E4)
        for (p in listOf(53, 57, 60, 64)) {
            epNotes.add(MidiNote(pitch = p, startBeat = 4f, durationBeats = 3.8f, velocity = 0.8f))
            epNotes.add(MidiNote(pitch = p, startBeat = 12f, durationBeats = 3.8f, velocity = 0.8f))
        }
        dao.insertClip(
            MidiClipEntity(
                trackId = epTrackId,
                projectId = projId,
                name = "Warm Velvet Chords",
                startBeat = 0f,
                durationBeats = 16f,
                notesJson = serializeNotes(epNotes)
            )
        )

        // 4. Traditional Guzheng Lead Melody (Chinese Guzheng Oriental Hook)
        val guzhengTrackId = dao.insertTrack(
            TrackEntity(
                projectId = projId,
                name = "Guzheng Zither (古筝)",
                instrumentType = InstrumentType.CHINESE_GUZHENG.id,
                volume = 0.9f,
                colorHex = 0xFFEF4444,
                trackOrder = 3
            )
        )
        // Pentatonic lead melody in A minor pentatonic: A4(69), C5(72), D5(74), E5(76), G5(79), A5(81)
        val melodyRhythm = listOf(
            Triple(69, 0f, 1f),
            Triple(72, 1f, 0.5f),
            Triple(74, 1.5f, 0.5f),
            Triple(76, 2f, 1.5f),
            Triple(79, 3.5f, 0.5f),
            Triple(81, 4f, 1f),
            Triple(79, 5f, 0.5f),
            Triple(76, 5.5f, 0.5f),
            Triple(74, 6f, 1.5f),
            Triple(72, 7.5f, 0.5f)
        )
        val gzNotes = mutableListOf<MidiNote>()
        for (rep in 0..1) {
            val offset = rep * 8f
            for (m in melodyRhythm) {
                gzNotes.add(MidiNote(pitch = m.first, startBeat = m.second + offset, durationBeats = m.third, velocity = 0.88f))
            }
        }
        dao.insertClip(
            MidiClipEntity(
                trackId = guzhengTrackId,
                projectId = projId,
                name = "Silk Road Melody",
                startBeat = 0f,
                durationBeats = 16f,
                notesJson = serializeNotes(gzNotes)
            )
        )

        // 5. Symphonic Strings Pad
        val stringsTrackId = dao.insertTrack(
            TrackEntity(
                projectId = projId,
                name = "Symphonic Strings",
                instrumentType = InstrumentType.ORCHESTRAL_STRINGS.id,
                volume = 0.78f,
                colorHex = 0xFFEC4899,
                trackOrder = 4
            )
        )
        val strNotes = mutableListOf<MidiNote>()
        for (p in listOf(69, 72, 76)) {
            strNotes.add(MidiNote(pitch = p, startBeat = 0f, durationBeats = 7.9f, velocity = 0.75f))
            strNotes.add(MidiNote(pitch = p, startBeat = 8f, durationBeats = 7.9f, velocity = 0.75f))
        }
        dao.insertClip(
            MidiClipEntity(
                trackId = stringsTrackId,
                projectId = projId,
                name = "Lush String Swells",
                startBeat = 0f,
                durationBeats = 16f,
                notesJson = serializeNotes(strNotes)
            )
        )

        // Collaborators
        dao.insertCollaborator(
            CollaboratorEntity(
                projectId = projId,
                name = "Lucas (Producer)",
                deviceName = "Android DAW",
                avatarColorHex = 0xFF38BDF8,
                currentActivity = "Mastering & Beat Arranging"
            )
        )
        dao.insertCollaborator(
            CollaboratorEntity(
                projectId = projId,
                name = "Sophia (Strings)",
                deviceName = "iPad Pro M4",
                avatarColorHex = 0xFFEC4899,
                currentActivity = "Orchestrating String Pad"
            )
        )
        dao.insertCollaborator(
            CollaboratorEntity(
                projectId = projId,
                name = "Kenji (Synthesist)",
                deviceName = "MacBook Pro M3",
                avatarColorHex = 0xFF10B981,
                currentActivity = "Designing Guzheng Filter"
            )
        )

        // Initial Revisions
        dao.insertRevision(
            RevisionEntity(
                projectId = projId,
                revisionNumber = 1,
                author = "Lucas",
                message = "Created track structure with 808 Drums and Slap Bass"
            )
        )
        dao.insertRevision(
            RevisionEntity(
                projectId = projId,
                revisionNumber = 2,
                author = "Sophia",
                message = "Layered lush symphonic strings in A Minor"
            )
        )
        dao.insertRevision(
            RevisionEntity(
                projectId = projId,
                revisionNumber = 3,
                author = "Kenji",
                message = "Programmed Guzheng lead melody & synced cloud project"
            )
        )

        return project
    }
}

package com.example.collab

import android.content.Context
import com.example.data.CollaboratorEntity
import com.example.data.ProjectEntity
import com.example.data.RevisionEntity
import com.example.data.StudioRepository
import com.example.data.TrackEntity
import com.example.midi.StandardMidiHelper
import kotlinx.coroutines.flow.Flow
import java.io.File

class CollaborationManager(
    private val repository: StudioRepository
) {
    fun getCollaborators(projectId: Long): Flow<List<CollaboratorEntity>> {
        return repository.getCollaborators(projectId)
    }

    fun getRevisions(projectId: Long): Flow<List<RevisionEntity>> {
        return repository.getRevisions(projectId)
    }

    suspend fun commitSyncSnapshot(
        projectId: Long,
        author: String,
        message: String
    ) {
        repository.commitRevision(projectId, author, message)
    }

    fun generateSharePayload(project: ProjectEntity): String {
        return "https://harmonia.studio/collab?session=${project.shareCode}&title=${project.title}"
    }

    suspend fun exportProjectJson(
        project: ProjectEntity,
        tracks: List<TrackEntity>,
        getNotesForTrack: (Long) -> List<com.example.midi.MidiNote>
    ): String {
        val exportTracks = tracks.map { track ->
            StandardMidiHelper.TrackExportData(
                trackName = track.name,
                instrument = com.example.audio.InstrumentType.fromId(track.instrumentType),
                notes = getNotesForTrack(track.id)
            )
        }
        return StandardMidiHelper.exportProjectToJson(
            title = project.title,
            bpm = project.bpm,
            timeSignature = project.timeSignature,
            shareCode = project.shareCode,
            tracks = exportTracks
        )
    }

    suspend fun exportMidiFile(
        context: Context,
        project: ProjectEntity,
        tracks: List<TrackEntity>,
        getNotesForTrack: (Long) -> List<com.example.midi.MidiNote>
    ): File {
        val exportTracks = tracks.map { track ->
            StandardMidiHelper.TrackExportData(
                trackName = track.name,
                instrument = com.example.audio.InstrumentType.fromId(track.instrumentType),
                notes = getNotesForTrack(track.id)
            )
        }
        val bytes = StandardMidiHelper.createStandardMidiFile(project.bpm, exportTracks)
        val file = File(context.cacheDir, "${project.title.replace(" ", "_")}.mid")
        file.writeBytes(bytes)
        return file
    }
}

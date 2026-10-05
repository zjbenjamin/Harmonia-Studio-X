package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudioDao {

    // Projects
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestProjectDirect(): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    // Tracks
    @Query("SELECT * FROM tracks WHERE projectId = :projectId ORDER BY trackOrder ASC")
    fun getTracksForProject(projectId: Long): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE projectId = :projectId ORDER BY trackOrder ASC")
    suspend fun getTracksForProjectDirect(projectId: Long): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity): Long

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Delete
    suspend fun deleteTrack(track: TrackEntity)

    // Midi Clips
    @Query("SELECT * FROM midi_clips WHERE projectId = :projectId")
    fun getClipsForProject(projectId: Long): Flow<List<MidiClipEntity>>

    @Query("SELECT * FROM midi_clips WHERE projectId = :projectId")
    suspend fun getClipsForProjectDirect(projectId: Long): List<MidiClipEntity>

    @Query("SELECT * FROM midi_clips WHERE trackId = :trackId")
    fun getClipsForTrack(trackId: Long): Flow<List<MidiClipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: MidiClipEntity): Long

    @Update
    suspend fun updateClip(clip: MidiClipEntity)

    @Delete
    suspend fun deleteClip(clip: MidiClipEntity)

    @Query("DELETE FROM midi_clips WHERE trackId = :trackId")
    suspend fun deleteClipsByTrack(trackId: Long)

    // Collaborators
    @Query("SELECT * FROM collaborators WHERE projectId = :projectId")
    fun getCollaborators(projectId: Long): Flow<List<CollaboratorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollaborator(collaborator: CollaboratorEntity): Long

    @Query("DELETE FROM collaborators WHERE projectId = :projectId")
    suspend fun clearCollaborators(projectId: Long)

    // Revisions
    @Query("SELECT * FROM revisions WHERE projectId = :projectId ORDER BY revisionNumber DESC")
    fun getRevisions(projectId: Long): Flow<List<RevisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevision(revision: RevisionEntity): Long
}

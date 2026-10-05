package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.audio.InstrumentType

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val bpm: Int = 120,
    val timeSignature: String = "4/4",
    val loopStartBeat: Float = 0f,
    val loopEndBeat: Float = 16f,
    val isLooping: Boolean = true,
    val shareCode: String = "HRMN-" + (1000..9999).random(),
    val cloudSyncStatus: String = "Synced", // Synced, Syncing, Offline
    val syncRevision: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tracks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val instrumentType: String, // from InstrumentType.id
    val volume: Float = 0.85f,
    val pan: Float = 0.0f, // -1.0 to 1.0
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val isArmed: Boolean = false,
    val colorHex: Long = 0xFF38BDF8,
    val trackOrder: Int = 0
)

@Entity(
    tableName = "midi_clips",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackId"])]
)
data class MidiClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val projectId: Long,
    val name: String,
    val startBeat: Float,
    val durationBeats: Float,
    val notesJson: String // Serialized JSON array of notes
)

@Entity(
    tableName = "collaborators",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class CollaboratorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val deviceName: String, // e.g. "MacBook Pro M3", "iPad Air", "Pixel 9"
    val avatarColorHex: Long,
    val isOnline: Boolean = true,
    val currentActivity: String = "Editing Melody"
)

@Entity(
    tableName = "revisions",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class RevisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val revisionNumber: Int,
    val author: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

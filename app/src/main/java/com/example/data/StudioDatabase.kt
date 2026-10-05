package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        TrackEntity::class,
        MidiClipEntity::class,
        CollaboratorEntity::class,
        RevisionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StudioDatabase : RoomDatabase() {

    abstract fun studioDao(): StudioDao

    companion object {
        @Volatile
        private var INSTANCE: StudioDatabase? = null

        fun getInstance(context: Context): StudioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudioDatabase::class.java,
                    "harmonia_studio.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

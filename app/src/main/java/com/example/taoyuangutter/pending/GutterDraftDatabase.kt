package com.example.taoyuangutter.pending

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DraftEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GutterDraftDatabase : RoomDatabase() {
    abstract fun draftDao(): DraftDao

    companion object {
        @Volatile
        private var instance: GutterDraftDatabase? = null

        fun getInstance(context: Context): GutterDraftDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GutterDraftDatabase::class.java,
                    "gutter_drafts.db"
                )
                    // Keep the existing synchronous repository API stable first.
                    .allowMainThreadQueries()
                    .build()
                    .also { instance = it }
            }
        }
    }
}

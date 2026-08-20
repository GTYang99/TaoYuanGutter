package com.example.taoyuangutter.pending

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DraftEntity::class],
    version = 2,
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
                    .addMigrations(MIGRATION_1_2)
                    // Keep the existing synchronous repository API stable first.
                    .allowMainThreadQueries()
                    .build()
                    .also { instance = it }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE gutter_session_drafts ADD COLUMN spi_typ TEXT")
            }
        }
    }
}

package com.example.taoyuangutter.pending

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DraftEntity::class],
    version = 3,
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
                    .addMigrations(MIGRATION_2_3)
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE gutter_session_drafts ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE gutter_session_drafts ADD COLUMN workflow_ownership TEXT NOT NULL DEFAULT 'LEGACY_SINGLE'")
                db.execSQL("UPDATE gutter_session_drafts SET created_at = saved_at WHERE created_at = 0")
            }
        }
    }
}

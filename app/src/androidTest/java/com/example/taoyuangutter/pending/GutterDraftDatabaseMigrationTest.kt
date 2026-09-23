package com.example.taoyuangutter.pending

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GutterDraftDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "gutter_drafts_migration_${System.currentTimeMillis()}.db"

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
        createVersion3Database()
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migration3To4AddsFalseSubmissionDefaultAndPreservesLegacyRow() {
        val helper = openDatabase(
            version = 4,
            callback = object : SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {
                    if (oldVersion < 4) GutterDraftDatabase.MIGRATION_3_4.migrate(db)
                }
            }
        )

        val db = helper.writableDatabase
        db.query(
            "SELECT has_submitted_store_ditch, waypoints_json " +
                "FROM gutter_session_drafts WHERE id = 923"
        ).use { cursor ->
            check(cursor.moveToFirst())
            assertFalse(cursor.getInt(0) != 0)
            assertEquals("[]", cursor.getString(1))
        }
        helper.close()
    }

    private fun createVersion3Database() {
        val helper = openDatabase(
            version = 3,
            callback = object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS gutter_session_drafts (" +
                            "id INTEGER NOT NULL PRIMARY KEY, " +
                            "created_at INTEGER NOT NULL, " +
                            "saved_at INTEGER NOT NULL, " +
                            "workflow_ownership TEXT NOT NULL, " +
                            "spi_typ TEXT, " +
                            "kind TEXT NOT NULL, " +
                            "is_offline INTEGER NOT NULL, " +
                            "is_single_point INTEGER NOT NULL, " +
                            "waypoints_json TEXT NOT NULL)"
                    )
                    db.execSQL(
                        "INSERT INTO gutter_session_drafts " +
                            "(id, created_at, saved_at, workflow_ownership, spi_typ, kind, " +
                            "is_offline, is_single_point, waypoints_json) " +
                            "VALUES (923, 1000, 2000, 'LEGACY_SINGLE', '1', 'gutter', 0, 0, '[]')"
                    )
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) = Unit
            }
        )
        helper.writableDatabase.close()
        helper.close()
    }

    private fun openDatabase(
        version: Int,
        callback: SupportSQLiteOpenHelper.Callback
    ): SupportSQLiteOpenHelper {
        return FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(callback)
                .build()
        )
    }
}

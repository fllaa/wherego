package com.flla.wherego.core.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncStateMigrationTest {
    /**
     * Room compares a migrated schema against its entities when the database opens and throws on
     * any drift — on the user's device, at launch, with no way back. `exportSchema` is off here,
     * so nothing else would catch a typo in the migration's DDL.
     */
    @Test
    fun migrationNineToTenBuildsTheTableRoomExpects() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val room = Room.inMemoryDatabaseBuilder(context, WheregoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val expected = try {
            room.openHelper.writableDatabase.columnsOf("sync_state")
        } finally {
            room.close()
        }

        val migrated = blankDatabase(context).use { db ->
            WheregoDatabase.MIGRATION_9_10.migrate(db)
            db.columnsOf("sync_state")
        }

        assertEquals(expected, migrated)
    }

    private fun blankDatabase(context: Context): SupportSQLiteDatabase =
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit
                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    },
                )
                .build(),
        ).writableDatabase

    /** Name, declared type, nullability and key position — what Room's validator compares. */
    private fun SupportSQLiteDatabase.columnsOf(table: String): List<String> =
        query("PRAGMA table_info(`$table`)").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        "${cursor.getString(1)} ${cursor.getString(2)} " +
                            "notNull=${cursor.getInt(3)} pk=${cursor.getInt(5)}",
                    )
                }
            }
        }
}

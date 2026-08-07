package com.ilaiyarasu.smartnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class], version = 4, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create new table with String id + isDeleted flag
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notes_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        category TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                // Copy every existing row, generating a fresh UUID-style string per row
                db.execSQL(
                    """
                    INSERT INTO notes_new (id, title, content, category, createdAt, updatedAt, isDeleted)
                    SELECT
                        lower(
                            hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' ||
                            substr(hex(randomblob(2)), 2) || '-' ||
                            substr('89ab', abs(random()) % 4 + 1, 1) || substr(hex(randomblob(2)), 2) || '-' ||
                            hex(randomblob(6))
                        ),
                        title, content, category, createdAt, updatedAt, 0
                    FROM notes
                    """.trimIndent()
                )

                // Replace old table with the new one
                db.execSQL("DROP TABLE notes")
                db.execSQL("ALTER TABLE notes_new RENAME TO notes")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN reminderTime INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "smartnote_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

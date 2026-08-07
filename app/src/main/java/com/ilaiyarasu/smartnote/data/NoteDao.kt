package com.ilaiyarasu.smartnote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND category = :category ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByCategory(category: String): Flow<List<Note>>

    @Query(
        "SELECT * FROM notes WHERE isDeleted = 0 AND (title LIKE '%' || :query || '%' " +
                "OR content LIKE '%' || :query || '%') ORDER BY isPinned DESC, updatedAt DESC"
    )
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): Note?

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesForSync(): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<Note>)

    @Update
    suspend fun updateNote(note: Note)

    @Query("UPDATE notes SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("UPDATE notes SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markDeleted(id: String, timestamp: Long)

    @Query("UPDATE notes SET reminderTime = :time WHERE id = :id")
    suspend fun setReminder(id: String, time: Long?)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}

package com.ilaiyarasu.smartnote.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    fun getNotesByCategory(category: String): Flow<List<Note>> =
        noteDao.getNotesByCategory(category)

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    suspend fun getNoteById(id: String): Note? = noteDao.getNoteById(id)

    suspend fun getAllNotesForSync(): List<Note> = noteDao.getAllNotesForSync()

    suspend fun insertNote(note: Note): Note {
        noteDao.insertNote(note)
        return note
    }

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun togglePinned(id: String, pinned: Boolean) = noteDao.setPinned(id, pinned)

    suspend fun deleteNote(note: Note) {
        noteDao.markDeleted(note.id, System.currentTimeMillis())
    }

    suspend fun setReminder(noteId: String, time: Long?) = noteDao.setReminder(noteId, time)

    suspend fun replaceAllNotes(notes: List<Note>) {
        noteDao.deleteAllNotes()
        noteDao.insertAll(notes)
    }

    suspend fun mergeNotes(remoteNotes: List<Note>) {
        val localNotes = noteDao.getAllNotesForSync().associateBy { it.id }
        val toInsert = mutableListOf<Note>()

        remoteNotes.forEach { remote ->
            val local = localNotes[remote.id]
            if (local == null || remote.updatedAt > local.updatedAt) {
                toInsert.add(remote)
            }
        }
        if (toInsert.isNotEmpty()) {
            noteDao.insertAll(toInsert)
        }
    }
}
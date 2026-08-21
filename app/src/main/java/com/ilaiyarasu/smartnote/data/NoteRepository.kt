package com.ilaiyarasu.smartnote.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    enum class SortOrder { DATE_DESC, DATE_ASC, TITLE_ASC }

    fun getNotesSorted(owner: String, category: String?, sortOrder: SortOrder): Flow<List<Note>> {
        return if (category == null) {
            when (sortOrder) {
                SortOrder.DATE_DESC -> noteDao.getAllNotesByDateDesc(owner)
                SortOrder.DATE_ASC -> noteDao.getAllNotesByDateAsc(owner)
                SortOrder.TITLE_ASC -> noteDao.getAllNotesByTitleAsc(owner)
            }
        } else {
            when (sortOrder) {
                SortOrder.DATE_DESC -> noteDao.getNotesByCategoryDateDesc(category, owner)
                SortOrder.DATE_ASC -> noteDao.getNotesByCategoryDateAsc(category, owner)
                SortOrder.TITLE_ASC -> noteDao.getNotesByCategoryTitleAsc(category, owner)
            }
        }
    }

    fun searchNotes(query: String, owner: String): Flow<List<Note>> = noteDao.searchNotesSortedByDate(query, owner)

    suspend fun getNoteById(id: String): Note? = noteDao.getNoteById(id)

    suspend fun getAllNotesForSync(owner: String): List<Note> = noteDao.getAllNotesForSync(owner)

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

    suspend fun replaceNotesByOwner(owner: String, notes: List<Note>) {
        noteDao.deleteNotesByOwner(owner)
        // Ensure every restored note is tagged with the current owner
        val notesWithOwner = notes.map { it.copy(ownerAccount = owner) }
        noteDao.insertAll(notesWithOwner)
    }

    suspend fun reassignNotes(oldOwner: String, newOwner: String) {
        noteDao.reassignOwner(oldOwner, newOwner)
    }

    suspend fun mergeNotes(remoteNotes: List<Note>, owner: String) {
        val localNotes = noteDao.getAllNotesForSync(owner).associateBy { it.id }
        val toInsert = mutableListOf<Note>()

        remoteNotes.forEach { remote ->
            // Ensure the remote note is tagged with the current owner before saving locally
            val noteWithOwner = remote.copy(ownerAccount = owner)
            val local = localNotes[remote.id]
            if (local == null || remote.updatedAt > local.updatedAt) {
                toInsert.add(noteWithOwner)
            }
        }
        if (toInsert.isNotEmpty()) {
            noteDao.insertAll(toInsert)
        }
    }
}
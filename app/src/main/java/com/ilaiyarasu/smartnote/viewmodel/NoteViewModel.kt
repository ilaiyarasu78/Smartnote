package com.ilaiyarasu.smartnote.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilaiyarasu.smartnote.data.Note
import com.ilaiyarasu.smartnote.data.NoteRepository
import com.ilaiyarasu.smartnote.util.AutoSyncScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(
    private val repository: NoteRepository,
    private val appContext: Context
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>> = combine(_searchQuery, _selectedCategory) { query, category ->
        query to category
    }.flatMapLatest { (query, category) ->
        when {
            query.isNotBlank() -> repository.searchNotes(query)
            category != null -> repository.getNotesByCategory(category)
            else -> repository.getAllNotes()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun addNote(title: String, content: String, category: String, reminderTime: Long? = null): String {
        val id = java.util.UUID.randomUUID().toString()
        viewModelScope.launch {
            repository.insertNote(
                Note(
                    id = id,
                    title = title,
                    content = content,
                    category = category,
                    reminderTime = reminderTime
                )
            )
            AutoSyncScheduler.scheduleSyncOnChange(appContext)
        }
        return id
    }

    fun updateNote(note: Note, title: String, content: String, category: String, reminderTime: Long? = null) {
        viewModelScope.launch {
            repository.updateNote(
                note.copy(
                    title = title,
                    content = content,
                    category = category,
                    updatedAt = System.currentTimeMillis(),
                    reminderTime = reminderTime
                )
            )
            AutoSyncScheduler.scheduleSyncOnChange(appContext)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
            AutoSyncScheduler.scheduleSyncOnChange(appContext)
        }
    }

    fun togglePinned(note: Note) {
        viewModelScope.launch {
            repository.togglePinned(note.id, !note.isPinned)
            AutoSyncScheduler.scheduleSyncOnChange(appContext)
        }
    }

    fun setReminder(noteId: String, time: Long?) {
        viewModelScope.launch {
            repository.setReminder(noteId, time)
            AutoSyncScheduler.scheduleSyncOnChange(appContext)
        }
    }

    fun restoreAllNotes(restoredNotes: List<Note>) {
        viewModelScope.launch {
            repository.replaceAllNotes(restoredNotes)
        }
    }

    suspend fun getNoteById(id: String): Note? = repository.getNoteById(id)
}
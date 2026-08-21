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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class NoteViewModel(
    private val repository: NoteRepository,
    private val appContext: Context
) : ViewModel() {

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    sealed class UiEvent {
        data class Error(val message: String) : UiEvent()
        object NoteSaved : UiEvent()
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _sortOrder = MutableStateFlow(NoteRepository.SortOrder.DATE_DESC)
    val sortOrder: StateFlow<NoteRepository.SortOrder> = _sortOrder.asStateFlow()

    private val _currentAccount = MutableStateFlow("local")
    val currentAccount: StateFlow<String> = _currentAccount.asStateFlow()

    fun setCurrentAccount(email: String) {
        _currentAccount.value = email
    }

    fun onSortOrderChange(order: NoteRepository.SortOrder) {
        _sortOrder.value = order
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>> = combine(
        _searchQuery, _selectedCategory, _currentAccount, _sortOrder
    ) { query, category, owner, sort ->
        DataState(query, category, owner, sort)
    }.flatMapLatest { state ->
        when {
            state.query.isNotBlank() -> repository.searchNotes(state.query, state.owner)
            else -> repository.getNotesSorted(state.owner, state.category, state.sortOrder)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private data class DataState(val query: String, val category: String?, val owner: String, val sortOrder: NoteRepository.SortOrder)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allNotesUnfiltered: StateFlow<List<Note>> = combine(_currentAccount, _sortOrder) { owner, sort ->
        owner to sort
    }.flatMapLatest { (owner, sort) ->
        repository.getNotesSorted(owner, null, sort)
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

    suspend fun addNote(title: String, content: String, category: String, reminderTime: Long? = null): String {
        val id = java.util.UUID.randomUUID().toString()
        try {
            repository.insertNote(
                Note(
                    id = id,
                    title = title,
                    content = content,
                    category = category,
                    reminderTime = reminderTime,
                    ownerAccount = _currentAccount.value
                )
            )
            AutoSyncScheduler.scheduleSyncOnChange(appContext)
            _events.emit(UiEvent.NoteSaved)
        } catch (e: Exception) {
            _events.emit(UiEvent.Error("Failed to save note: ${e.message}"))
        }
        return id
    }

    suspend fun updateNote(note: Note, title: String, content: String, category: String, reminderTime: Long? = null) {
        try {
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
            _events.emit(UiEvent.NoteSaved)
        } catch (e: Exception) {
            _events.emit(UiEvent.Error("Failed to update note: ${e.message}"))
        }
    }

    suspend fun deleteNote(note: Note) {
        try {
            repository.deleteNote(note)
            AutoSyncScheduler.scheduleSyncOnChange(appContext)
        } catch (e: Exception) {
            _events.emit(UiEvent.Error("Failed to delete note: ${e.message}"))
        }
    }

    suspend fun togglePinned(note: Note) {
        try {
            repository.togglePinned(note.id, !note.isPinned)
            AutoSyncScheduler.scheduleSyncOnChange(appContext)
        } catch (e: Exception) {
            _events.emit(UiEvent.Error("Failed to toggle pin: ${e.message}"))
        }
    }

    suspend fun setReminder(noteId: String, time: Long?) {
        try {
            repository.setReminder(noteId, time)
            AutoSyncScheduler.scheduleSyncOnChange(appContext)
        } catch (e: Exception) {
            _events.emit(UiEvent.Error("Failed to set reminder: ${e.message}"))
        }
    }

    suspend fun restoreNotesByOwner(owner: String, restoredNotes: List<Note>) {
        repository.replaceNotesByOwner(owner, restoredNotes)
    }

    suspend fun migrateNotesToNewAccount(oldOwner: String, newOwner: String) {
        repository.reassignNotes(oldOwner, newOwner)
        _currentAccount.value = newOwner
    }

    suspend fun getNoteById(id: String): Note? = repository.getNoteById(id)
}
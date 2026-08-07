package com.ilaiyarasu.smartnote

import android.app.Application
import com.ilaiyarasu.smartnote.data.NoteDatabase
import com.ilaiyarasu.smartnote.data.NoteRepository
import com.ilaiyarasu.smartnote.util.PreferencesManager

class SmartnoteApplication : Application() {
    val database by lazy { NoteDatabase.getDatabase(this) }
    val repository by lazy { NoteRepository(database.noteDao()) }
    val preferencesManager by lazy { PreferencesManager(this) }
}
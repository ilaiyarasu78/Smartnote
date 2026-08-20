package com.ilaiyarasu.smartnote.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ilaiyarasu.smartnote.data.NoteDatabase
import com.ilaiyarasu.smartnote.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = NoteDatabase.getDatabase(context)
            val notes = db.noteDao().getAllNotesForAllAccounts()
            notes.filter { it.reminderTime != null && it.reminderTime!! > System.currentTimeMillis() }
                .forEach { note ->
                    ReminderScheduler.scheduleReminder(
                        context, 
                        note.id, 
                        note.title, 
                        note.content, 
                        note.reminderTime!!
                    )
                }
        }
    }
}

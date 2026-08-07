package com.ilaiyarasu.smartnote.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ilaiyarasu.smartnote.data.NoteDatabase
import com.ilaiyarasu.smartnote.data.NoteRepository

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("AutoSyncDebug", "Worker started")
        val prefs = PreferencesManager(applicationContext)
        
        if (!prefs.isAutoSyncEnabled) {
            Log.d("AutoSyncDebug", "Auto-sync disabled — skipping")
            return Result.success()
        }

        val driveBackupManager = DriveBackupManager(applicationContext)
        val account = driveBackupManager.getLastSignedInAccount()
            ?: driveBackupManager.silentSignIn()

        if (account == null) {
            Log.d("AutoSyncDebug", "No signed-in account found — skipping backup")
            return Result.failure()
        }

        val dao = NoteDatabase.getDatabase(applicationContext).noteDao()
        val repository = NoteRepository(dao)
        val notes = repository.getAllNotesForSync()

        return driveBackupManager.backupNotes(account, notes).fold(
            onSuccess = {
                Log.d("AutoSyncDebug", "Backup succeeded")
                prefs.lastSyncTimestamp = System.currentTimeMillis()
                Result.success()
            },
            onFailure = { e ->
                Log.d("AutoSyncDebug", "Backup failed: ${e.message}")
                Result.retry()
            }
        )
    }
}

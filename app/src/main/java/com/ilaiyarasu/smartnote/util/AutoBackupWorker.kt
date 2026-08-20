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
        
        // 1. Determine which account to sync
        // Prefer the email passed in the task data to prevent cross-account leaks during switches
        val targetEmail = inputData.getString("target_email")
        val account = if (targetEmail != null) {
            // If a specific account was requested, ensure we can still get a valid client for it
            driveBackupManager.getLastSignedInAccount()?.takeIf { it.email == targetEmail }
                ?: driveBackupManager.silentSignIn()?.takeIf { it.email == targetEmail }
        } else {
            driveBackupManager.getLastSignedInAccount() ?: driveBackupManager.silentSignIn()
        }

        if (account == null) {
            Log.d("AutoSyncDebug", "Target account ($targetEmail) not available — skipping")
            return Result.failure()
        }

        val owner = account.email ?: "local"
        val dao = NoteDatabase.getDatabase(applicationContext).noteDao()
        val repository = NoteRepository(dao)

        // 1. AUTO-RESTORE: Download latest from cloud and MERGE (not replace)
        driveBackupManager.restoreNotes(account).fold(
            onSuccess = { remoteNotes ->
                Log.d("AutoSyncDebug", "Auto-Restore: Merging ${remoteNotes.size} notes from cloud")
                // Use mergeNotes to prevent deleting local work that hasn't synced yet
                repository.mergeNotes(remoteNotes, owner)
            },
            onFailure = { 
                Log.d("AutoSyncDebug", "Auto-Restore: No cloud backup found or failed: ${it.message}")
            }
        )

        // 2. AUTO-BACKUP: Upload final merged state back to cloud
        val notes = repository.getAllNotesForSync(owner)
        if (notes.isEmpty()) {
            Log.d("AutoSyncDebug", "No local notes to back up. Sync complete.")
            return Result.success()
        }

        return driveBackupManager.backupNotes(account, notes).fold(
            onSuccess = {
                Log.d("AutoSyncDebug", "Auto-Backup: Success")
                prefs.lastSyncTimestamp = System.currentTimeMillis()
                Result.success()
            },
            onFailure = { e ->
                Log.d("AutoSyncDebug", "Auto-Backup: Failed: ${e.message}")
                Result.failure()
            }
        )
    }
}

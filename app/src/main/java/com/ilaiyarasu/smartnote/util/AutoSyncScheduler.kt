package com.ilaiyarasu.smartnote.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object AutoSyncScheduler {

    private const val PERIODIC_WORK_NAME = "smartnote_auto_backup_periodic"
    private const val ON_CHANGE_WORK_NAME = "smartnote_auto_backup_on_change"

    fun schedulePeriodicSync(context: Context) {
        val prefs = PreferencesManager(context)
        val currentEmail = prefs.lastSignedInEmail ?: return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val data = Data.Builder()
            .putString("target_email", currentEmail)
            .build()

        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(15, TimeUnit.MINUTES)
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleSyncOnChange(context: Context) {
        val prefs = PreferencesManager(context)
        val currentEmail = prefs.lastSignedInEmail ?: return // don't auto-sync if guest

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val data = Data.Builder()
            .putString("target_email", currentEmail)
            .build()

        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setInputData(data)
            .setInitialDelay(5, TimeUnit.SECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ON_CHANGE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(ON_CHANGE_WORK_NAME)
    }

    fun triggerImmediateSync(context: Context) {
        val prefs = PreferencesManager(context)
        val currentEmail = prefs.lastSignedInEmail

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val data = Data.Builder()
        currentEmail?.let { data.putString("target_email", it) }

        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setInputData(data.build())
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "smartnote_immediate_sync",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

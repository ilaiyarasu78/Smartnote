package com.ilaiyarasu.smartnote.util

import android.content.Context
import androidx.work.Constraints
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
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleSyncOnChange(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setInitialDelay(15, TimeUnit.SECONDS)
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
}

package com.moneytracker.app.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.moneytracker.app.MoneyTrackerApp
import com.moneytracker.app.data.local.SessionStore
import com.moneytracker.app.data.local.WidgetSnapshotStore
import com.moneytracker.app.data.remote.SupabaseClient
import com.moneytracker.app.data.repository.MoneyRepository
import java.util.concurrent.TimeUnit

class WidgetSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as? MoneyTrackerApp)?.container?.repository
            ?: run {
                val sessionStore = SessionStore(applicationContext)
                val widgetStore = WidgetSnapshotStore(applicationContext)
                val supabase = SupabaseClient(sessionStore)
                MoneyRepository(applicationContext, sessionStore, widgetStore, supabase)
            }

        return runCatching {
            repository.refreshWidgetSnapshot()
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "money_tracker_widget_periodic_sync"
        private const val IMMEDIATE_WORK_NAME = "money_tracker_widget_immediate_sync"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<WidgetSyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<WidgetSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

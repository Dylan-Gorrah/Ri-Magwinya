package com.rimagwinya.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Sends whatever the phone queued while it had no signal.
 *
 * Runs only when there is a connection, and backs off if the queue is still
 * stuck — a student in a dead spot should not be flattening their battery
 * retrying.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sync: SyncRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when (sync.replay()) {
        SyncOutcome.Done, SyncOutcome.Refused -> Result.success()
        // Refusals stay in the queue with a reason for the screen to show;
        // retrying them would fail the same way for ever.
        SyncOutcome.Offline -> Result.retry()
    }

    companion object {
        private const val NAME = "rimagwinya-sync"

        /** Safe to call often: the same named work is kept, not stacked. */
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                    )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build(),
            )
        }
    }
}

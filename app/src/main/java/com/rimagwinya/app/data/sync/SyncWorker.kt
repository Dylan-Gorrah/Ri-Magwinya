package com.rimagwinya.app.data.sync

import android.util.Log
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


private const val TAG = "RmSync"

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

    // Reference: Google. 2026. Getting started with WorkManager. [Online]. Available at:
    // <https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started>
    // [Accessed 5 November 2026].
    override suspend fun doWork(): Result {
        Log.d(TAG, "worker: back online, replaying the queue (attempt ${runAttemptCount + 1})")
        return when (val outcome = sync.replay()) {
            SyncOutcome.Done, SyncOutcome.Refused -> {
                Log.d(TAG, "worker: finished ($outcome)")
                Result.success()
            }
            // Refusals stay in the queue with a reason for the screen to show;
            // retrying them would fail the same way for ever.
            SyncOutcome.Offline -> {
                Log.d(TAG, "worker: still no connection, retrying later")
                Result.retry()
            }
        }
    }

    companion object {
        private const val NAME = "rimagwinya-sync"

        /** Safe to call often: the same named work is kept, not stacked. */
        fun schedule(context: Context) {
            Log.d(TAG, "worker: scheduled for when the phone is online")
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

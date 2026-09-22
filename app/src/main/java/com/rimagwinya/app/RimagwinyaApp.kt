package com.rimagwinya.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.rimagwinya.app.data.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Hilt needs one of these to build the dependency graph. Everything else the
 * app does at startup should stay out of here - a slow Application class is a
 * slow cold start on exactly the budget phones this app has to run on.
 *
 * The one addition is WorkManager, which has to be told how to build workers
 * that take injected dependencies.
 */
@HiltAndroidApp
class RimagwinyaApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Anything queued by a previous run goes as soon as there is signal.
        SyncWorker.schedule(this)
    }
}

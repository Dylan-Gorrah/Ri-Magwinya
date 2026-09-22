package com.rimagwinya.app.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/**
 * Dispatchers are injected rather than referenced directly so tests can
 * substitute a test dispatcher and control time. A repository that calls
 * Dispatchers.IO itself is a repository that cannot be tested without
 * waiting for real threads.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Injected for the same reason: "is this break still open?" and "is the
     * weather cache stale?" are only testable if a test can set the time.
     */
    @Provides
    fun clock(): java.time.Clock = java.time.Clock.systemUTC()
}

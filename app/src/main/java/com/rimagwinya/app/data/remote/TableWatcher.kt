package com.rimagwinya.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Something in this table changed — fetch again."
 *
 * For screens where refetching is simpler and safer than patching: the order
 * queue, a student's orders, the slots. Emits once on every (re)connect as
 * well, because changes made while the socket was down were never sent.
 * Realtime respects RLS, so a student watching `orders` hears only their own.
 *
 * Never throws: a broken socket retries with backoff and the screen keeps
 * whatever it last fetched.
 */
@Singleton
class TableWatcher @Inject constructor(
    private val client: SupabaseClient,
) {
    private val counter = AtomicInteger()

    /** [rowFilter] is a realtime filter such as `id=eq.<uuid>`. */
    fun changes(table: String, rowFilter: String? = null): Flow<Unit> = channelFlow {
        // Unique per subscription: two screens watching one table must not
        // share (and then tear down) the same channel.
        val channel = client.channel("$table-${counter.incrementAndGet()}")
        val actions = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            this.table = table
            if (rowFilter != null) filter = rowFilter
        }

        launch { actions.collect { send(Unit) } }
        launch {
            channel.status.collect { status ->
                if (status == RealtimeChannel.Status.SUBSCRIBED) send(Unit)
            }
        }

        channel.subscribe()
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { client.realtime.removeChannel(channel) }
        }
    }
        .retryWhen { _, attempt ->
            delay(minOf(30_000L, 2_000L shl attempt.toInt().coerceAtMost(4)))
            true
        }
        // A burst of changes (a staff member clearing the queue) is one refetch.
        .conflate()
}

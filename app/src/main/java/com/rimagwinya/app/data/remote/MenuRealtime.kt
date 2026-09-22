package com.rimagwinya.app.data.remote

import com.rimagwinya.app.domain.model.MenuChange
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Changes to `menu_items`, live, over the Supabase realtime socket.
 *
 * Realtime respects RLS, and the menu is readable signed out, so this works
 * before sign-in too. The subscription lives exactly as long as the flow is
 * collected and the channel is removed when collection stops.
 */
@Singleton
class MenuRealtime @Inject constructor(
    private val client: SupabaseClient,
) {
    fun changes(): Flow<MenuChange> = channelFlow {
        val channel = client.channel("menu-items")
        val actions = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "menu_items"
        }

        launch {
            actions.collect { action ->
                val change = when (action) {
                    is PostgresAction.Update -> action.record.toMenuUpdate()
                    // Options are not in the payload, so a new or removed
                    // item means fetching the menu again.
                    is PostgresAction.Insert, is PostgresAction.Delete -> MenuChange.Reload
                    else -> null
                }
                change?.let { send(it) }
            }
        }

        // Anything that changed while the socket was down was never sent.
        launch {
            channel.status.collect { status ->
                if (status == RealtimeChannel.Status.SUBSCRIBED) send(MenuChange.Reload)
            }
        }

        channel.subscribe()
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { client.realtime.removeChannel(channel) }
        }
    }
}

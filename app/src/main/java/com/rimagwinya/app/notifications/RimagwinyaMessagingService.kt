package com.rimagwinya.app.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rimagwinya.app.data.repository.PushTokenRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Incoming pushes, and the token that lets the server send them.
 *
 * The token goes to `profiles.fcm_token`, which the Edge Function reads when
 * an order is marked ready. Signing out clears it, because on a shared or
 * handed-down phone the next person should not get the last person's order
 * notifications.
 */
@AndroidEntryPoint
class RimagwinyaMessagingService : FirebaseMessagingService() {

    @Inject lateinit var tokens: PushTokenRepository
    @Inject lateinit var scope: CoroutineScope

    override fun onNewToken(token: String) {
        scope.launch { tokens.register(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // The server sends a notification payload for the tray plus data for
        // the deep link. Android shows the tray one itself when the app is in
        // the background; this is the foreground case.
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val channel = when (message.data["type"]) {
            "wallet_topup" -> Notifications.CHANNEL_WALLET
            "low_stock" -> Notifications.CHANNEL_STOCK
            else -> Notifications.CHANNEL_ORDERS
        }

        scope.launch {
            if (!tokens.notificationsWanted()) return@launch
            Notifications.show(
                context = applicationContext,
                title = title,
                body = body,
                channel = channel,
                orderId = message.data["order_id"],
            )
        }
    }
}

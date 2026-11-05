package com.rimagwinya.app.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rimagwinya.app.data.repository.PushTokenRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject


private const val TAG = "RmPush"

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

    // Reference: Google. 2026. Firebase: Receive messages in an Android app. [Online]. Available at:
    // <https://firebase.google.com/docs/cloud-messaging/android/receive> [Accessed 5 November 2026].
    override fun onNewToken(token: String) {
        // The token is an address for this phone: logged as "new", never in full.
        Log.d(TAG, "token: Firebase issued a new token, saving it to the profile")
        scope.launch { tokens.register(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // The server sends a notification payload for the tray plus data for
        // the deep link. Android shows the tray one itself when the app is in
        // the background; this is the foreground case.
        Log.d(TAG, "message: received, type=${message.data["type"]}, order=${message.data["order_id"]}")
        val title = message.notification?.title ?: message.data["title"] ?: run {
            Log.e(TAG, "message: no title, nothing to show")
            return
        }
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val channel = when (message.data["type"]) {
            "wallet_topup" -> Notifications.CHANNEL_WALLET
            "low_stock" -> Notifications.CHANNEL_STOCK
            else -> Notifications.CHANNEL_ORDERS
        }

        scope.launch {
            if (!tokens.notificationsWanted()) {
                Log.d(TAG, "message: notifications are off in settings, not shown")
                return@launch
            }
            Log.d(TAG, "message: showing it on the $channel channel")
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

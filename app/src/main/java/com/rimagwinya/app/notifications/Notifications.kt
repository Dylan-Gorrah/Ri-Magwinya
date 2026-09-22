package com.rimagwinya.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rimagwinya.app.MainActivity
import com.rimagwinya.app.R

/**
 * Notification channels and the one place a notification is actually shown.
 *
 * Three channels, because the phone's own settings are the right place for
 * "tell me when my order is ready, but do not wake me for stock warnings".
 */
object Notifications {

    const val CHANNEL_ORDERS = "orders"
    const val CHANNEL_WALLET = "wallet"
    const val CHANNEL_STOCK = "stock"

    /** Extra on the launch intent, read by MainActivity to open an order. */
    const val EXTRA_ORDER_ID = "order_id"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        listOf(
            Triple(CHANNEL_ORDERS, R.string.channel_orders, NotificationManager.IMPORTANCE_HIGH),
            Triple(CHANNEL_WALLET, R.string.channel_wallet, NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CHANNEL_STOCK, R.string.channel_stock, NotificationManager.IMPORTANCE_LOW),
        ).forEach { (id, nameRes, importance) ->
            manager.createNotificationChannel(
                NotificationChannel(id, context.getString(nameRes), importance)
            )
        }
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Shows one. Tapping it opens the app and, when the push carried an
     * order id, that order.
     */
    fun show(
        context: Context,
        title: String,
        body: String,
        channel: String = CHANNEL_ORDERS,
        orderId: String? = null,
    ) {
        if (!hasPermission(context)) return

        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply { orderId?.let { putExtra(EXTRA_ORDER_ID, it) } }

        val pending = PendingIntent.getActivity(
            context,
            orderId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_brand_mark)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(orderId?.hashCode() ?: 1, notification)
        }
    }
}

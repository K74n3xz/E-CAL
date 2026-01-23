package net.k74n3xz.ecal.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_ID = "ecal_reminders"
    private const val CHANNEL_NAME = "E•CAL Reminders"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = nm.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val ch = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminder notifications from E•CAL"
                setShowBadge(true)
            }
            nm.createNotificationChannel(ch)
        }
    }

    @androidx.annotation.RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNotification(context: Context, id: Int, title: String, text: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title)
            .setContentText(text).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(id, builder.build())
        }
    }
}
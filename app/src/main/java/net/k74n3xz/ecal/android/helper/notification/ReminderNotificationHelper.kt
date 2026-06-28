package net.k74n3xz.ecal.android.helper.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.android.constant.Notification
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotificationHelper @Inject constructor(@param:ApplicationContext private val context: Context) {
    // TODO: Define how notification operations should fail when NotificationManager is unavailable.
    val notificationManager: NotificationManager =
        context.applicationContext.getSystemService(NotificationManager::class.java)

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureChannel() {
        val channel = NotificationChannel(
            /* id = */ Notification.Channel.REMINDER_CHANNEL_ID,
            /* name = */ context.getString(R.string.notification_channel_name_reminders),
            /* importance = */ NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description_reminders)
            lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
            // TODO: Decide whether reminders may bypass Do Not Disturb and, if so, request policy access.
        }
        notificationManager.createNotificationChannel(channel)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS, conditional = true)
    fun showNotification(tag: String?, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ensureChannel()
        }

        val builder = NotificationCompat.Builder(
            /* context = */ context.applicationContext,
            /* channelId = */ Notification.Channel.REMINDER_CHANNEL_ID
        ).apply {
            setSmallIcon(R.mipmap.ic_launcher_round)
            setContentTitle(title)
            setContentText(text)
            setCategory(NotificationCompat.CATEGORY_REMINDER)
            priority = NotificationCompat.PRIORITY_HIGH
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        notificationManager.notify(tag, id, builder.build())
    }
}
package net.k74n3xz.ecal.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import net.k74n3xz.ecal.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@param:ApplicationContext private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "ECAL-Reminders"
    }

    // TODO: Handle a missing NotificationManager service before creating channels or notifications.
    val notificationManager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureChannel() {
        val channel = NotificationChannel(
            /* id = */ CHANNEL_ID,
            /* name = */ context.getString(R.string.notification_channel_name_reminders),
            /* importance = */ NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description_reminders)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
            // TODO: Decide whether reminder notifications should request permission to bypass Do Not Disturb.
        }
        notificationManager.createNotificationChannel(channel)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS, conditional = true)
    fun showNotification(id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ensureChannel()
        }

        val builder = NotificationCompat.Builder(context.applicationContext, CHANNEL_ID).apply {
            setSmallIcon(R.mipmap.ic_launcher_round)
            setContentTitle(title)
            setContentText(text)
            priority = NotificationCompat.PRIORITY_HIGH
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        notificationManager.notify(id, builder.build())
    }
}
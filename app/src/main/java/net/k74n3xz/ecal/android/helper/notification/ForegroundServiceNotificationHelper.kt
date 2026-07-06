package net.k74n3xz.ecal.android.helper.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import net.k74n3xz.ecal.R
import javax.inject.Inject
import javax.inject.Singleton
import net.k74n3xz.ecal.android.constant.Notification as NotificationConstant

@Singleton
class ForegroundServiceNotificationHelper @Inject constructor(@param:ApplicationContext private val context: Context) {
    // TODO: Define how notification operations should fail when NotificationManager is unavailable.
    val notificationManager: NotificationManager =
        context.applicationContext.getSystemService(NotificationManager::class.java)

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureChannel() {
        val channel = NotificationChannel(
            /* id = */ NotificationConstant.Channel.FOREGROUND_SERVICE_CHANNEL_ID,
            /* name = */ context.getString(R.string.notification_channel_name_foreground_service),
            /* importance = */ NotificationManager.IMPORTANCE_LOW
        ).apply {
            description =
                context.getString(R.string.notification_channel_description_foreground_service)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun buildNotification(title: String, text: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ensureChannel()
        }

        return NotificationCompat.Builder(
            /* context = */ context.applicationContext,
            /* channelId = */ NotificationConstant.Channel.FOREGROUND_SERVICE_CHANNEL_ID
        ).apply {
            setSmallIcon(R.mipmap.ic_launcher_round)
            setContentTitle(title)
            setContentText(text)
            setOngoing(true)
            setCategory(NotificationCompat.CATEGORY_SERVICE)
            priority = NotificationCompat.PRIORITY_LOW
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }.build()
    }
}
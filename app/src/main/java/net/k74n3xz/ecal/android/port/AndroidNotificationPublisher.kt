package net.k74n3xz.ecal.android.port

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.android.helper.notification.ReminderNotificationHelper
import net.k74n3xz.ecal.core.application.port.NotificationPublisher
import javax.inject.Inject
import javax.inject.Singleton
import net.k74n3xz.ecal.android.constant.Notification as NotificationConstant

@Singleton
class AndroidNotificationPublisher @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val reminderNotificationHelper: ReminderNotificationHelper
) : NotificationPublisher {
    private companion object {
        private const val TAG: String = "AndroidNotificationPublisher"
    }

    override fun publish(id: Long, description: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && context.applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(
                TAG,
                "publish: ${Manifest.permission.POST_NOTIFICATIONS} is not granted, skipped."
            )
        } else {
            reminderNotificationHelper.showNotification(
                tag = NotificationConstant.Tag.REMINDER_NOTIFICATION_TAG(id),
                id = NotificationConstant.Id.REMINDER_NOTIFICATION_ID,
                title = context.applicationContext.getString(R.string.notification_default_title_reminder),
                text = description
            )
        }
    }
}
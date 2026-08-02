package net.k74n3xz.ecal.platform.port

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.core.application.port.outbound.platform.NotificationPublisher
import net.k74n3xz.ecal.platform.android.constant.Notification as NotificationConstant
import net.k74n3xz.ecal.platform.android.helper.notification.ReminderNotificationHelper

@Singleton
internal class AndroidNotificationPublisher @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val reminderNotificationHelper: ReminderNotificationHelper
) : NotificationPublisher {
    private companion object {
        private const val TAG: String = "AndroidNotificationPublisher"
    }

    override fun publish(id: Long, description: String): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "publish: ${Manifest.permission.POST_NOTIFICATIONS} is not granted, skipped.")
            false
        } else {
            reminderNotificationHelper.showNotification(
                tag = NotificationConstant.Tag.getReminderNotificationTag(id),
                id = NotificationConstant.Id.REMINDER_NOTIFICATION_ID,
                title = context.applicationContext.getString(R.string.notification_default_title_reminder),
                text = description
            )
            true
        }
}

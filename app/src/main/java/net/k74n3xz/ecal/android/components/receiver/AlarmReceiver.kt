package net.k74n3xz.ecal.android.components.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.android.constant.Notification as NotificationConstant
import net.k74n3xz.ecal.android.helper.notification.ReminderNotificationHelper
import net.k74n3xz.ecal.data.calendar.repository.AlarmRepository
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG: String = "AlarmReceiver"
    }

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var reminderNotificationHelper: ReminderNotificationHelper

    private val receiverScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        receiverScope.launch {
            val alarmInstanceId: Long? = intent.data?.lastPathSegment?.toLong()

            val title =
                context.applicationContext.getString(R.string.notification_default_title_reminder)
            val text =
                alarmInstanceId?.let { alarmRepository.getAlarmDescriptionByAlarmInstanceId(it) }
                    ?: context.getString(R.string.notification_default_text_reminder)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(
                    TAG,
                    "onReceive: ${Manifest.permission.POST_NOTIFICATIONS} is not granted, skipped."
                )
            } else {
                reminderNotificationHelper.showNotification(
                    tag = alarmInstanceId?.let {
                        NotificationConstant.Tag.REMINDER_NOTIFICATION_TAG(
                            it
                        )
                    },
                    id = NotificationConstant.Id.REMINDER_NOTIFICATION_ID,
                    title = title,
                    text = text
                )
            }

            if (alarmInstanceId != null) {
                alarmRepository.deactivateAlarmInstance(alarmInstanceId)
            }

            pendingResult.finish()
        }
    }
}
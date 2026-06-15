package net.k74n3xz.ecal.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(@param:ApplicationContext private val context: Context) {
    // TODO: Move reminder intent constants to a shared contract object.
    companion object {
        const val INTENT_ID_KEY = "id"
        const val INTENT_TITLE_KEY = "title"
        const val INTENT_TEXT_KEY = "text"

        private const val ACTION_REMINDER = "net.k74n3xz.ecal.ACTION_REMINDER"
    }

    // TODO: Handle a missing AlarmManager service before scheduling or cancelling reminders.
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    private fun buildIntent(reminderId: Long, title: String? = null, text: String? = null): Intent =
        Intent(context.applicationContext, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            // TODO: Preserve the full reminder ID instead of using a hash that can collide.
            putExtra(INTENT_ID_KEY, reminderId.hashCode())
            putExtra(INTENT_TITLE_KEY, title)
            putExtra(INTENT_TEXT_KEY, text)
        }

    private fun buildPendingIntent(reminderId: Long, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            /* context = */ context.applicationContext,
            /* requestCode = */ reminderId.hashCode(),  // TODO: Use a collision-free request code for reminder IDs.
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun scheduleReminder(reminderId: Long, triggerAtMillis: Long, title: String?, text: String?) {
        val pendingIntent = buildPendingIntent(reminderId, buildIntent(reminderId, title, text))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(
                /* type = */ AlarmManager.RTC_WAKEUP,
                /* triggerAtMillis = */ triggerAtMillis,
                /* operation = */ pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                /* type = */ AlarmManager.RTC_WAKEUP,
                /* triggerAtMillis = */ triggerAtMillis,
                /* operation = */ pendingIntent
            )
        }
    }

    fun cancelReminder(reminderId: Long) {
        val pendingIntent = buildPendingIntent(reminderId, buildIntent(reminderId))
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
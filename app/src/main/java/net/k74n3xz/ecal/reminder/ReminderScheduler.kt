package net.k74n3xz.ecal.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission

object ReminderScheduler {
    private const val ACTION_REMINDER = "net.k74n3xz.ecal.ACTION_REMINDER"
    private const val EXTRA_REMINDER_ID = "extra_reminder_id"
    private const val EXTRA_TITLE = "extra_title"
    private const val EXTRA_TEXT = "extra_text"

    private fun buildIntent(
        context: Context, reminderId: Long, title: String?, text: String?
    ): Intent {
        return Intent(context.applicationContext, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_TEXT, text)
        }
    }

    private fun pendingIntentFor(
        context: Context, reminderId: Long, title: String?, text: String?
    ): PendingIntent {
        val intent = buildIntent(context, reminderId, title, text)
        val requestCode = (reminderId xor (reminderId ushr 32)).toInt()
        val flags =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context.applicationContext, requestCode, intent, flags)
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun scheduleReminder(
        context: Context, reminderId: Long, triggerAtMillis: Long, title: String?, text: String?
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntentFor(context, reminderId, title, text)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } catch (ex: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntentFor(context, reminderId, null, null)
        am.cancel(pi)
        pi.cancel()
    }
}
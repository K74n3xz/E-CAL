package net.k74n3xz.ecal.android.helper.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import net.k74n3xz.ecal.android.components.receiver.AlarmReceiver
import net.k74n3xz.ecal.android.constant.Action
import net.k74n3xz.ecal.android.constant.RequestCode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmHelper @Inject constructor(@param:ApplicationContext private val context: Context) {
    // TODO: Define how alarm operations should fail when AlarmManager is unavailable.
    private val alarmManager: AlarmManager =
        context.applicationContext.getSystemService(AlarmManager::class.java)

    private fun buildIntent(alarmId: Long): Intent =
        Intent(context.applicationContext, AlarmReceiver::class.java).apply {
            action = Action.ACTION_ALARM
            // TODO: Share alarm URI construction and parsing to keep sender and receiver formats aligned.
            data = Uri.Builder()
                .scheme("net.k74n3xz.ecal")
                .authority("alarm")
                .path("/id/$alarmId")
                .build()
        }

    private fun buildPendingIntent(intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            /* context = */ context.applicationContext,
            /* requestCode = */ RequestCode.ALARM_REQUEST_CODE,
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun schedule(id: Long, triggerAtMillis: Long) {
        val pendingIntent = buildPendingIntent(buildIntent(id))

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

    fun cancel(id: Long) {
        val pendingIntent = buildPendingIntent(buildIntent(id))
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
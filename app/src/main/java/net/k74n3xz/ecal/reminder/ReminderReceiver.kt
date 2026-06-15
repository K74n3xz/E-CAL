package net.k74n3xz.ecal.reminder

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import net.k74n3xz.ecal.R
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        // TODO: Define how to handle reminder broadcasts that do not include a valid reminder ID.
        val id = intent.getIntExtra(ReminderScheduler.INTENT_ID_KEY, 0)
        val title = intent.getStringExtra(ReminderScheduler.INTENT_TITLE_KEY)
            ?: context.getString(R.string.notification_default_title_reminder)
        val text =
            intent.getStringExtra(ReminderScheduler.INTENT_TEXT_KEY)
                ?: context.getString(R.string.notification_default_text_reminder)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && context.applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(
                "ReminderReceiver",
                "onReceive: ${Manifest.permission.POST_NOTIFICATIONS} is not granted."
            )
        } else {
            notificationHelper.showNotification(id, title, text)
        }
    }
}
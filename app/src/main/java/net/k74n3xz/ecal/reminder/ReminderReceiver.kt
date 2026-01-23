package net.k74n3xz.ecal.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        try {
            val id = intent.getLongExtra("extra_reminder_id", -1L)
            val title = intent.getStringExtra("extra_title")
            val text = intent.getStringExtra("extra_text")
            val notifId =
                if (id >= 0L) (id xor (id ushr 32)).toInt() else System.currentTimeMillis().toInt()
            NotificationHelper.ensureChannel(context)
            NotificationHelper.showNotification(
                context, notifId, title ?: "Reminder", text ?: "You have a reminder"
            )
        } catch (e: Exception) {
            Log.e("ReminderReceiver", "error handling reminder: ${e.message}")
        }
    }
}
package net.k74n3xz.ecal.platform.port

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.core.application.port.out.platform.EmailSender
import net.k74n3xz.ecal.platform.android.constant.Notification as NotificationConstant
import net.k74n3xz.ecal.platform.android.helper.notification.ReminderNotificationHelper

@Singleton
class AndroidEmailSender @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val reminderNotificationHelper: ReminderNotificationHelper
) : EmailSender {
    private companion object {
        private const val TAG: String = "AndroidEmailSender"
    }

    override fun send(
        id: Int,
        subject: String,
        text: String,
        receivers: List<String>,
        attachments: List<String>
    ): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        Log.w(TAG, "send: ${Manifest.permission.POST_NOTIFICATIONS} is not granted, skipped.")
        false
    } else {
        val intent = when {
            attachments.isEmpty() -> Intent(Intent.ACTION_SENDTO, "mailto:".toUri())

            attachments.size == 1 -> Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, attachments.single().toUri())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            else -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, ArrayList(attachments.map { it.toUri() }))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }.apply {
            putExtra(Intent.EXTRA_EMAIL, receivers.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val pendingIntent = PendingIntent.getActivity(
            context.applicationContext,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        reminderNotificationHelper.showEmailNotification(
            tag = NotificationConstant.Tag.getReminderEmailNotificationTag(id),
            id = NotificationConstant.Id.REMINDER_EMAIL_NOTIFICATION_ID,
            title = context.applicationContext.getString(R.string.notification_default_title_reminder),
            text = context.applicationContext.getString(R.string.notification_text_send_reminder_email),
            pendingIntent = pendingIntent
        )
        true
    }
}

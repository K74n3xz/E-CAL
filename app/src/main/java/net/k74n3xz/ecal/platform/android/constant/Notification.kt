package net.k74n3xz.ecal.platform.android.constant

object Notification {
    object Channel {
        const val REMINDER_CHANNEL_ID: String = "ECAL-Reminder"
        const val FOREGROUND_SERVICE_CHANNEL_ID: String = "ECAL-Foreground_Service"
    }

    object Tag {
        private const val REMINDER_NOTIFICATION_TAG_PREFIX: String = "reminder:"
        private const val REMINDER_EMAIL_NOTIFICATION_TAG_PREFIX: String = "email:"
        fun getReminderNotificationTag(id: Long): String = "$REMINDER_NOTIFICATION_TAG_PREFIX$id"
        fun getReminderEmailNotificationTag(id: Int): String = "$REMINDER_EMAIL_NOTIFICATION_TAG_PREFIX$id"
    }

    object Id {
        const val REMINDER_NOTIFICATION_ID: Int = 1
        const val REMINDER_AUDIO_PLAYER_NOTIFICATION_ID: Int = 2
        const val REMINDER_EMAIL_NOTIFICATION_ID: Int = 3
        const val FOREGROUND_SERVICE_NOTIFICATION_ID: Int = 1001
    }
}

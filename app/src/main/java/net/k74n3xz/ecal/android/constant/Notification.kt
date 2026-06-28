package net.k74n3xz.ecal.android.constant

object Notification {
    object Channel {
        const val REMINDER_CHANNEL_ID: String = "ECAL-Reminder"
        const val FOREGROUND_SERVICE_CHANNEL_ID: String = "ECAL-Foreground_Service"
    }

    object Tag {
        private const val REMINDER_NOTIFICATION_TAG_PREFIX: String = "reminder:"

        fun REMINDER_NOTIFICATION_TAG(id: Long): String = "$REMINDER_NOTIFICATION_TAG_PREFIX$id"
    }

    object Id {
        const val REMINDER_NOTIFICATION_ID: Int = 1
        const val FOREGROUND_SERVICE_NOTIFICATION_ID: Int = 1001
    }
}
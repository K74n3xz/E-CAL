package net.k74n3xz.ecal.platform.port

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.k74n3xz.ecal.platform.android.helper.notification.ReminderNotificationHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33, Config.TARGET_SDK], application = Application::class)
class AndroidEmailSenderTest {
    private lateinit var application: Application
    private lateinit var notificationManager: NotificationManager
    private lateinit var sender: AndroidEmailSender

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        notificationManager = application.getSystemService(NotificationManager::class.java)
        notificationManager.cancelAll()
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        sender = AndroidEmailSender(application, ReminderNotificationHelper(application))
    }

    @After
    fun tearDown() {
        notificationManager.cancelAll()
    }

    @Test
    fun noAttachments_postsSendToIntentWithMessageFields() {
        assertTrue(
            sender.send(
                id = 11,
                subject = "Subject",
                text = "Body",
                receivers = listOf("first@example.com", "second@example.com"),
                attachments = emptyList()
            )
        )

        val intent = postedContentIntent()
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals(Uri.parse("mailto:"), intent.data)
        assertEquals("Subject", intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals("Body", intent.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals(
            listOf("first@example.com", "second@example.com"),
            intent.getStringArrayExtra(Intent.EXTRA_EMAIL)?.toList()
        )
    }

    @Test
    fun oneAttachment_postsSendIntentWithReadGrant() {
        val attachment = Uri.parse("content://net.k74n3xz.ecal.fileprovider/attachments/agenda.pdf")

        assertTrue(sender.send(12, "Subject", "Body", listOf("user@example.com"), listOf(attachment.toString())))

        val intent = postedContentIntent()
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("*/*", intent.type)
        assertEquals(attachment, intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun multipleAttachments_postsSendMultipleIntentPreservingOrder() {
        val attachments = listOf(
            Uri.parse("content://net.k74n3xz.ecal.fileprovider/attachments/first.txt"),
            Uri.parse("content://net.k74n3xz.ecal.fileprovider/attachments/second.txt")
        )

        assertTrue(
            sender.send(
                13,
                "Subject",
                "Body",
                listOf("user@example.com"),
                attachments.map(Uri::toString)
            )
        )

        val intent = postedContentIntent()
        assertEquals(Intent.ACTION_SEND_MULTIPLE, intent.action)
        assertEquals("*/*", intent.type)
        assertEquals(
            attachments,
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        )
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun deniedNotificationPermission_returnsFalseWithoutPosting() {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        assertFalse(sender.send(14, "Subject", "Body", listOf("user@example.com"), emptyList()))
        assertTrue(shadowOf(notificationManager).allNotifications.isEmpty())
    }

    private fun postedContentIntent(): Intent {
        val notifications = shadowOf(notificationManager).allNotifications
        assertEquals(1, notifications.size)
        return shadowOf(notifications.single().contentIntent).savedIntent
    }
}

package net.k74n3xz.ecal.core.model.property.alarm

import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.core.model.Attendee
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ActionTest {
    private val attachment = Attachment(
        id = 3L,
        description = "Agenda",
        name = "agenda.txt",
        mimeType = "text/plain",
        sizeBytes = 128L
    )
    private val attendee = Attendee(id = 5L, name = "User", email = "user@example.com")

    @Test
    fun audioAction_supportsOptionalAttachment() {
        assertNull(Action.Audio().attach)
        assertEquals(attachment, Action.Audio(attachment).attach)
    }

    @Test
    fun displayAction_preservesDescription() {
        assertEquals("Reminder", Action.Display("Reminder").description)
    }

    @Test
    fun emailAction_rejectsEmptyAttendees() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            Action.Email(description = "Description", summary = "Summary", attendee = emptyList())
        }

        assertEquals(
            "When the action is `EMAIL`, the alarm must include one or more `ATTENDEE` properties.",
            error.message
        )
    }

    @Test
    fun emailAction_preservesAttendeesAndAttachments() {
        val attendees = listOf(attendee, Attendee(id = 6L, name = "Second User", email = "second@example.com"))
        val attachments = listOf(attachment)

        val action = Action.Email(
            description = "Description",
            summary = "Summary",
            attendee = attendees,
            attach = attachments
        )

        assertEquals("Description", action.description)
        assertEquals("Summary", action.summary)
        assertEquals(attendees, action.attendee)
        assertEquals(attachments, action.attach)
    }

    @Test
    fun emailAction_defaultsToNoAttachments() {
        val action = Action.Email(
            description = "Description",
            summary = "Summary",
            attendee = listOf(attendee)
        )

        assertNull(action.attach)
    }
}

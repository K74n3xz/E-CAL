package net.k74n3xz.ecal.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttendeeTest {
    @Test
    fun attendee_appliesDefaultsAndPreservesEmail() {
        val attendee = Attendee(email = "user@example.com")

        assertNull(attendee.id)
        assertNull(attendee.name)
        assertNull(attendee.description)
        assertEquals("user@example.com", attendee.email)
    }
}

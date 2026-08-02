package net.k74n3xz.ecal.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttachmentTest {
    @Test
    fun attachment_preservesFieldsAndNullableValues() {
        val attachment = Attachment(
            id = null,
            description = null,
            name = "agenda.txt",
            mimeType = "text/plain",
            sizeBytes = 128L
        )

        assertNull(attachment.id)
        assertNull(attachment.description)
        assertEquals("agenda.txt", attachment.name)
        assertEquals("text/plain", attachment.mimeType)
        assertEquals(128L, attachment.sizeBytes)
    }
}

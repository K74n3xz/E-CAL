package net.k74n3xz.ecal.core.application.impl.service

import java.io.File
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.application.testing.ApplicationUseCaseTestData
import net.k74n3xz.ecal.core.application.testing.RecordingAttachmentRepository
import net.k74n3xz.ecal.core.application.testing.RecordingAttendeeRepository
import net.k74n3xz.ecal.core.application.testing.RecordingEventRepository
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.core.model.Attendee
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class EventQueryServiceTest {
    @Test
    fun findEventByUid_forwardsUidAndResult() = runTest {
        val repository = RecordingEventRepository(mutableListOf())
        val expected = ApplicationUseCaseTestData.event()
        repository.found = expected

        val actual = EventQueryServiceImpl(repository).findEventByUid("event-1")

        assertSame(expected, actual)
        assertEquals(listOf("event-1"), repository.requestedUids)
    }

    @Test
    fun observeEventsOverlappingRange_forwardsRangeAndFlow() = runTest {
        val repository = RecordingEventRepository(mutableListOf())
        val expected = listOf(ApplicationUseCaseTestData.event())
        repository.observed = flowOf(expected)
        val start = ZonedDateTime.of(2026, 7, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val end = start.plusMonths(1)

        val flow = EventQueryServiceImpl(repository).observeEventsOverlappingRange(start, end)

        assertSame(repository.observed, flow)
        assertEquals(expected, flow.first())
        assertEquals(listOf(start to end), repository.observedRanges)
    }
}

class AttachmentQueryServiceTest {
    private val attachment = Attachment(1, "agenda", "agenda.pdf", "application/pdf", 42)

    @Test
    fun findAttachmentById_forwardsIdAndResult() = runTest {
        val repository = RecordingAttachmentRepository()
        repository.found = attachment

        val actual = AttachmentQueryServiceImpl(repository).findAttachmentById(1)

        assertSame(attachment, actual)
        assertEquals(listOf(1L), repository.requestedIds)
    }

    @Test
    fun observeAvailableAttachments_returnsRepositoryFlow() = runTest {
        val repository = RecordingAttachmentRepository()
        repository.observed = flowOf(listOf(attachment))

        val flow = AttachmentQueryServiceImpl(repository).observeAvailableAttachments()

        assertSame(repository.observed, flow)
        assertEquals(listOf(attachment), flow.first())
    }

    @Test
    fun observeAvailableAttachmentsByMimeTopLevelType_forwardsTypeAndFlow() = runTest {
        val repository = RecordingAttachmentRepository()
        repository.observedByMimeTopLevelType = flowOf(listOf(attachment))

        val flow = AttachmentQueryServiceImpl(repository).observeAvailableAttachmentsByMimeTopLevelType("audio")

        assertSame(repository.observedByMimeTopLevelType, flow)
        assertEquals(listOf("audio"), repository.requestedMimeTopLevelTypes)
        assertEquals(listOf(attachment), flow.first())
    }

    @Test
    fun openAttachmentById_forwardsIdAndStream() = runTest {
        val repository = RecordingAttachmentRepository()
        val file = File.createTempFile("attachment-query-service", ".tmp")
        file.writeText("content")
        val expected = file.inputStream()
        repository.openedAttachment = expected

        try {
            val actual = AttachmentQueryServiceImpl(repository).openAttachmentById(1)

            assertSame(expected, actual)
            assertEquals(listOf(1L), repository.openedIds)
        } finally {
            expected.close()
            file.delete()
        }
    }
}

class AttendeeQueryServiceTest {
    private val attendee = Attendee(id = 2, name = "Ada", email = "ada@example.com")

    @Test
    fun findAttendeeById_forwardsIdAndResult() = runTest {
        val repository = RecordingAttendeeRepository()
        repository.found = attendee

        val actual = AttendeeQueryServiceImpl(repository).findAttendeeById(2)

        assertSame(attendee, actual)
        assertEquals(listOf(2L), repository.requestedIds)
    }

    @Test
    fun observeAllAttendees_returnsRepositoryFlow() = runTest {
        val repository = RecordingAttendeeRepository()
        repository.observed = flowOf(listOf(attendee))

        val flow = AttendeeQueryServiceImpl(repository).observeAllAttendees()

        assertSame(repository.observed, flow)
        assertEquals(listOf(attendee), flow.first())
    }
}

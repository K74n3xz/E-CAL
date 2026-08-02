package net.k74n3xz.ecal.core.application.impl.usecase

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.application.testing.ApplicationUseCaseTestData
import net.k74n3xz.ecal.core.application.testing.RecordingAlarmRepository
import net.k74n3xz.ecal.core.application.testing.RecordingAttachmentRepository
import net.k74n3xz.ecal.core.application.testing.RecordingAudioPlayer
import net.k74n3xz.ecal.core.application.testing.RecordingEmailSender
import net.k74n3xz.ecal.core.application.testing.RecordingPublisher
import net.k74n3xz.ecal.core.application.testing.RecordingReconciler
import net.k74n3xz.ecal.core.application.testing.RecordingTimeZoneProvider
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.core.model.property.alarm.Action
import org.junit.Assert.assertEquals
import org.junit.Test

class HandleDueAlarmsUseCaseTest {
    private class Fixture(due: List<Pair<LongArray, Action>> = emptyList()) {
        val calls = mutableListOf<String>()
        val repository = RecordingAlarmRepository(calls, due)
        val attachmentRepository = RecordingAttachmentRepository(calls)
        val reconciler = RecordingReconciler(calls)
        val publisher = RecordingPublisher(calls)
        val audioPlayer = RecordingAudioPlayer(calls)
        val emailSender = RecordingEmailSender(calls)
        val timeZoneProvider = RecordingTimeZoneProvider()
        val useCase = HandleDueAlarmsUseCaseImpl(
            repository,
            attachmentRepository,
            timeZoneProvider,
            reconciler,
            audioPlayer,
            publisher,
            emailSender
        )
    }

    private val now = ApplicationUseCaseTestData.now

    @Test
    fun emptyBatch_queriesRepositoryOnly() = runTest {
        val fixture = Fixture()

        fixture.useCase(now)

        assertEquals(listOf("getDue:$now"), fixture.calls)
        assertEquals(0, fixture.reconciler.callCount)
        assertEquals(1, fixture.timeZoneProvider.currentReadCount)
    }

    @Test
    fun displayOccurrences_arePublishedAndProcessed_thenReconciledOnce() = runTest {
        val fixture = Fixture(
            listOf(
                longArrayOf(1, 2) to Action.Display("first"),
                longArrayOf(3) to Action.Display("second")
            )
        )

        fixture.useCase(now)

        assertEquals(
            listOf(
                "getDue:$now",
                "publish:1:first",
                "process:1",
                "publish:2:first",
                "process:2",
                "publish:3:second",
                "process:3",
                "reconcile"
            ),
            fixture.calls
        )
        assertEquals(1, fixture.reconciler.callCount)
        assertEquals(1, fixture.timeZoneProvider.currentReadCount)
        assertEquals(List(3) { java.time.ZoneOffset.ofHours(8) }, fixture.repository.processedTimeZones)
    }

    @Test
    fun emptyGroupsAreIgnoredButNonEmptyBatchReconcilesOnce() = runTest {
        val fixture = Fixture(
            listOf(
                longArrayOf() to Action.Display("unused"),
                longArrayOf(4) to Action.Display("actual"),
                longArrayOf() to Action.Display("also-unused")
            )
        )

        fixture.useCase(now)

        assertEquals(listOf("getDue:$now", "publish:4:actual", "process:4", "reconcile"), fixture.calls)
        assertEquals(1, fixture.reconciler.callCount)
    }

    @Test
    fun allActionTypesDispatchAndForwardPlatformResults() = runTest {
        val audioAttachment = Attachment(7, null, "alarm.mp3", "audio/mpeg", 42)
        val audio = Action.Audio(audioAttachment)
        val email = Action.Email(
            description = "body",
            summary = "subject",
            attendee = listOf(Attendee(email = "ada@example.com"))
        )
        val fixture = Fixture(
            listOf(
                longArrayOf(1) to audio,
                longArrayOf(2) to Action.Display("display"),
                longArrayOf(3) to email
            )
        )
        fixture.audioPlayer.result = false
        fixture.publisher.result = false

        fixture.useCase(now)

        assertEquals(listOf(7L), fixture.audioPlayer.attachmentIds)
        assertEquals(
            listOf(
                RecordingEmailSender.Arguments(
                    id = 3,
                    subject = "subject",
                    text = "body",
                    receivers = listOf("ada@example.com"),
                    attachments = emptyList()
                )
            ),
            fixture.emailSender.arguments
        )
        assertEquals(listOf(1L to false, 2L to false, 3L to true), fixture.repository.processedResults)
        assertEquals(
            listOf(
                "getDue:$now",
                "audio",
                "process:1",
                "publish:2:display",
                "process:2",
                "email",
                "process:3",
                "reconcile"
            ),
            fixture.calls
        )
    }

    @Test
    fun audioWithoutAttachment_usesDefaultAudio() = runTest {
        val fixture = Fixture(listOf(longArrayOf(1) to Action.Audio()))

        fixture.useCase(now)

        assertEquals(listOf<Long?>(null), fixture.audioPlayer.attachmentIds)
        assertEquals(listOf(1L to true), fixture.repository.processedResults)
    }

    @Test
    fun emailAttachments_generateTokensInOrderAndSkipUnavailableFiles() = runTest {
        val attachments = listOf(
            Attachment(7, null, "first.txt", "text/plain", 1),
            Attachment(8, null, "missing.txt", "text/plain", 2),
            Attachment(9, null, "last.txt", "text/plain", 3)
        )
        val email = Action.Email(
            description = "body",
            summary = "subject",
            attendee = listOf(
                Attendee(email = "first@example.com"),
                Attendee(email = "second@example.com")
            ),
            attach = attachments
        )
        val fixture = Fixture(listOf(longArrayOf(0x1_0000_0002L) to email))
        fixture.attachmentRepository.sharingTokens += mapOf(
            7L to "content://attachments/7",
            8L to null,
            9L to "content://attachments/9"
        )

        fixture.useCase(now)

        assertEquals(listOf(7L, 8L, 9L), fixture.attachmentRepository.sharingTokenIds)
        assertEquals(
            listOf(
                RecordingEmailSender.Arguments(
                    id = 3,
                    subject = "subject",
                    text = "body",
                    receivers = listOf("first@example.com", "second@example.com"),
                    attachments = listOf("content://attachments/7", "content://attachments/9")
                )
            ),
            fixture.emailSender.arguments
        )
        assertEquals(listOf(0x1_0000_0002L to true), fixture.repository.processedResults)
    }

    @Test
    fun emailSenderFalse_isForwardedAsUnhandled() = runTest {
        val email = Action.Email(
            description = "body",
            summary = "subject",
            attendee = listOf(Attendee(email = "ada@example.com"))
        )
        val fixture = Fixture(listOf(longArrayOf(3) to email))
        fixture.emailSender.result = false

        fixture.useCase(now)

        assertEquals(listOf(3L to false), fixture.repository.processedResults)
    }

    @Test
    fun attachmentTokenFailure_stopsBeforeSendingOrProcessing() = runTest {
        val failure = IllegalStateException("token failed")
        val email = Action.Email(
            description = "body",
            summary = "subject",
            attendee = listOf(Attendee(email = "ada@example.com")),
            attach = listOf(Attachment(7, null, "agenda.txt", "text/plain", 1))
        )
        val fixture = Fixture(listOf(longArrayOf(3) to email))
        fixture.attachmentRepository.failure = failure

        val thrown = runCatching { fixture.useCase(now) }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(emptyList<RecordingEmailSender.Arguments>(), fixture.emailSender.arguments)
        assertEquals(emptyList<Pair<Long, Boolean>>(), fixture.repository.processedResults)
        assertEquals(0, fixture.reconciler.callCount)
    }

    @Test
    fun groupWithoutOccurrences_doesNotReconcile() = runTest {
        val fixture = Fixture(listOf(longArrayOf() to Action.Display("unused")))

        fixture.useCase(now)

        assertEquals(listOf("getDue:$now"), fixture.calls)
        assertEquals(0, fixture.reconciler.callCount)
    }

    @Test
    fun publishFailure_isPropagatedAndOccurrenceIsNotProcessed() = runTest {
        val failure = IllegalStateException("notification failed")
        val fixture = Fixture(listOf(longArrayOf(1) to Action.Display("text")))
        fixture.publisher.failure = failure

        val thrown = runCatching { fixture.useCase(now) }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("getDue:$now", "publish:1:text"), fixture.calls)
        assertEquals(0, fixture.reconciler.callCount)
    }

    @Test
    fun processFailure_isPropagatedAndStopsBatchBeforeReconcile() = runTest {
        val failure = IllegalStateException("process failed")
        val fixture = Fixture(listOf(longArrayOf(1, 2) to Action.Display("text")))
        fixture.repository.processFailure = failure

        val thrown = runCatching { fixture.useCase(now) }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("getDue:$now", "publish:1:text", "process:1"), fixture.calls)
        assertEquals(0, fixture.reconciler.callCount)
    }

    @Test
    fun timeZoneFailureStopsBeforeDispatchingActions() = runTest {
        val failure = IllegalStateException("time zone failed")
        val fixture = Fixture(listOf(longArrayOf(1) to Action.Display("text")))
        fixture.timeZoneProvider.failure = failure

        val thrown = runCatching { fixture.useCase(now) }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(listOf("getDue:$now"), fixture.calls)
        assertEquals(0, fixture.reconciler.callCount)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun concurrentInvocations_areSerialized() = runTest {
        val releaseFirstQuery = CompletableDeferred<Unit>()
        val fixture = Fixture()
        fixture.repository.firstDueQueryRelease = releaseFirstQuery

        val first = async { fixture.useCase(now) }
        runCurrent()
        assertEquals(1, fixture.repository.dueQueryCount)

        val second = async { fixture.useCase(now) }
        runCurrent()
        assertEquals(1, fixture.repository.dueQueryCount)

        releaseFirstQuery.complete(Unit)
        first.await()
        second.await()

        assertEquals(2, fixture.repository.dueQueryCount)
        assertEquals(listOf("getDue:$now", "getDue:$now"), fixture.calls)
    }
}

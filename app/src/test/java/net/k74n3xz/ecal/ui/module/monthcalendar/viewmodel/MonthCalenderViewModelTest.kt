package net.k74n3xz.ecal.ui.module.monthcalendar.viewmodel

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.k74n3xz.ecal.core.application.port.`in`.service.EventQueryService
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.core.model.property.event.EventTiming
import net.k74n3xz.ecal.core.preference.api.PreferenceRepository
import net.k74n3xz.ecal.testutils.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MonthCalenderViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var preferences: CalendarPreferenceRepository
    private lateinit var queryService: RecordingCalendarEventQueryService
    private lateinit var viewModel: MonthCalenderViewModel

    @Before
    fun setUp() {
        preferences = CalendarPreferenceRepository()
        queryService = RecordingCalendarEventQueryService()
        viewModel = MonthCalenderViewModel(preferences, queryService)
    }

    @Test
    fun setSelectedDate_updatesState() {
        val selected = LocalDate.of(2026, 7, 20)

        viewModel.setSelectedDate(selected)

        assertEquals(selected, viewModel.selectedDate.value)
    }

    @Test
    fun busyDates_queryCoversPreviousThroughNextMonth() = runTest(mainDispatcherRule.dispatcher) {
        preferences.timeZone.value = ZoneId.of("UTC")
        viewModel.setSelectedDate(LocalDate.of(2026, 7, 20))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.busyDatesRecently.collect {}
        }
        runCurrent()

        assertEquals(
            CalendarQuery(
                ZonedDateTime.parse("2026-06-01T00:00:00Z[UTC]"),
                ZonedDateTime.parse("2026-08-31T23:59:59.999999999Z[UTC]")
            ),
            queryService.queries.single()
        )
    }

    @Test
    fun busyDates_expandsTimedAndAllDayRangesAndDeduplicatesDates() = runTest(mainDispatcherRule.dispatcher) {
        preferences.timeZone.value = ZoneId.of("Asia/Hong_Kong")
        viewModel.setSelectedDate(LocalDate.of(2026, 7, 20))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.busyDatesRecently.collect {}
        }
        runCurrent()

        queryService.events.value = listOf(
            Event(
                uid = "timed",
                schedule = EventTiming.Timed.RangeTiming(
                    Instant.parse("2026-07-20T15:00:00Z"),
                    Instant.parse("2026-07-21T01:00:00Z")
                )
            ),
            Event(
                uid = "all-day",
                schedule = EventTiming.AllDay.RangeDateTiming(
                    LocalDate.of(2026, 7, 21),
                    LocalDate.of(2026, 7, 23)
                )
            )
        )
        runCurrent()

        assertEquals(
            setOf(
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 21),
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 23)
            ),
            viewModel.busyDatesRecently.value
        )
    }

    @Test
    fun timezoneChange_requeriesAndRecomputesTimedDates() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.setSelectedDate(LocalDate.of(2026, 7, 20))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.busyDatesRecently.collect {}
        }
        queryService.events.value = listOf(
            Event(
                uid = "boundary",
                schedule = EventTiming.Timed.InstantTiming(Instant.parse("2026-07-20T23:30:00Z"))
            )
        )
        runCurrent()
        assertEquals(setOf(LocalDate.of(2026, 7, 20)), viewModel.busyDatesRecently.value)

        preferences.timeZone.value = ZoneId.of("Asia/Hong_Kong")
        runCurrent()

        assertEquals(2, queryService.queries.size)
        assertEquals(setOf(LocalDate.of(2026, 7, 21)), viewModel.busyDatesRecently.value)
    }

    @Test
    fun eventsOnSelectedDate_queriesActualDstDayBounds() = runTest(mainDispatcherRule.dispatcher) {
        preferences.timeZone.value = ZoneId.of("America/New_York")
        viewModel.setSelectedDate(LocalDate.of(2026, 3, 8))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventsOnSelectedDate.collect {}
        }
        runCurrent()

        assertEquals(
            CalendarQuery(
                ZonedDateTime.parse("2026-03-08T00:00:00-05:00[America/New_York]"),
                ZonedDateTime.parse("2026-03-08T23:59:59.999999999-04:00[America/New_York]")
            ),
            queryService.queries.single()
        )
    }

    @Test
    fun selectedDateChange_requeriesAndPublishesRepositoryEvents() = runTest(mainDispatcherRule.dispatcher) {
        val selectedDate = viewModel.selectedDate.value.plusDays(1)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventsOnSelectedDate.collect {}
        }
        runCurrent()
        val event = Event(
            uid = "selected",
            schedule = EventTiming.AllDay.SingleDateTiming(selectedDate)
        )
        queryService.events.value = listOf(event)
        viewModel.setSelectedDate(selectedDate)
        runCurrent()

        assertEquals(2, queryService.queries.size)
        assertEquals(listOf(event), viewModel.eventsOnSelectedDate.value)
    }
}

private data class CalendarQuery(val start: ZonedDateTime, val end: ZonedDateTime)

private class CalendarPreferenceRepository : PreferenceRepository {
    override val timeZone = MutableStateFlow(ZoneId.of("UTC"))
}

private class RecordingCalendarEventQueryService : EventQueryService {
    val events = MutableStateFlow<List<Event>>(emptyList())
    val queries = mutableListOf<CalendarQuery>()

    override suspend fun findEventByUid(uid: String): Event? = null

    override fun observeEventsOverlappingRange(rangeStart: ZonedDateTime, rangeEnd: ZonedDateTime): Flow<List<Event>> {
        queries += CalendarQuery(rangeStart, rangeEnd)
        return events
    }
}

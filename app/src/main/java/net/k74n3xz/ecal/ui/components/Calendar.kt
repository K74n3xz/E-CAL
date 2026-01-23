package net.k74n3xz.ecal.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import net.k74n3xz.ecal.ui.components.CalendarType.DAY
import net.k74n3xz.ecal.ui.components.CalendarType.MONTH
import net.k74n3xz.ecal.ui.components.CalendarType.WEEK
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.absoluteValue

private val LocalDate.yearMonth: YearMonth
    get() = YearMonth.of(year, month)

private fun LocalDate.firstDayOfWeek(firstDay: DayOfWeek): LocalDate =
    minusDays((dayOfWeek.value - firstDay.value).toLong())

enum class CalendarType { MONTH, WEEK, DAY }

@Composable
fun ModeSelectionRow(
    locale: Locale,
    mode: CalendarType,
    onModeChanged: (CalendarType) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = CalendarType.entries.map { entry ->
        entry.name.lowercase().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                locale
            ) else it.toString()
        }
    }

    SingleChoiceSegmentedButtonRow(modifier) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == mode.ordinal,
                onClick = { onModeChanged(CalendarType.entries[index]) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index, count = options.size
                ),
                icon = {},
                label = { Text(label) })
        }
    }
}

@Composable
fun Calendar(
    type: CalendarType,
    firstDayOfWeek: DayOfWeek,
    locale: Locale,
    selectedDate: LocalDate,
    eventDays: Set<LocalDate>,
    onDateChanged: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()

    var indicator by rememberSaveable { mutableStateOf(today) }

    Column(
        modifier.animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "${indicator.year} / ${indicator.monthValue}",
            modifier = Modifier.padding(vertical = 16.dp),
            style = MaterialTheme.typography.titleLarge
        )

        if (type == MONTH || type == WEEK) {
            WeekDayHeader(firstDayOfWeek, locale, weekDayCellModifier = Modifier.weight(1f))
        }

        val initialPage = Int.MAX_VALUE / 2
        val pagerState = rememberPagerState(initialPage) { Int.MAX_VALUE }
        val animationScope = rememberCoroutineScope()
        LaunchedEffect(type) {
            onDateChanged(indicator)
            pagerState.scrollToPage(
                initialPage + when (type) {
                    MONTH -> ChronoUnit.MONTHS
                    WEEK -> ChronoUnit.WEEKS
                    DAY -> ChronoUnit.DAYS
                }.between(today, indicator).toInt()
            )
        }
        LaunchedEffect(type) {
            snapshotFlow { pagerState.currentPage }.collect {
                indicator = when (type) {
                    MONTH -> today.plusMonths((it - initialPage).toLong()).let { date ->
                        if (date.yearMonth.lengthOfMonth() < indicator.dayOfMonth) date else date.withDayOfMonth(
                            indicator.dayOfMonth
                        )
                    }

                    WEEK -> today.plusWeeks((it - initialPage).toLong())
                        .firstDayOfWeek(firstDayOfWeek).plusDays(
                            (indicator.dayOfWeek.value - 1).toLong()
                        )

                    DAY -> today.plusDays((it - initialPage).toLong())
                }
            }
        }
        HorizontalPager(pagerState) { page ->
            val dateCellModifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
            val pageModifier = Modifier.graphicsLayer {
                val pageOffset =
                    ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                alpha = lerp(start = 0.2f, stop = 1f, fraction = 1f - pageOffset.coerceIn(0f, 1f))
            }
            when (type) {
                MONTH -> MonthMatrix(
                    today.yearMonth.plusMonths((page - initialPage).toLong()),
                    firstDayOfWeek,
                    selectedDate,
                    eventDays,
                    {
                        if (it.yearMonth != indicator.yearMonth) {
                            animationScope.launch {
                                pagerState.animateScrollToPage(
                                    initialPage + ChronoUnit.MONTHS.between(
                                        today.yearMonth, it.yearMonth
                                    ).toInt()
                                )
                            }
                        }
                        indicator = it
                        onDateChanged(it)
                    },
                    modifier = pageModifier,
                    dateCellModifier = dateCellModifier
                )

                WEEK -> WeekList(
                    today.plusWeeks((page - initialPage).toLong()).firstDayOfWeek(firstDayOfWeek),
                    selectedDate,
                    eventDays,
                    {
                        indicator = it
                        onDateChanged(it)
                    },
                    modifier = pageModifier,
                    dateCellModifier = dateCellModifier
                )

                DAY -> DayInfo(
                    today.plusDays((page - initialPage).toLong()),
                    eventDays.contains(indicator),
                    modifier = pageModifier,
                    dateCellModifier = dateCellModifier
                )
            }
        }
    }
}

@Composable
private fun WeekDayHeader(
    firstDayOfWeek: DayOfWeek,
    locale: Locale,
    modifier: Modifier = Modifier,
    weekDayCellModifier: Modifier = Modifier
) {
    Row(modifier) {
        for (i in 0 until 7) {
            Box(
                weekDayCellModifier.aspectRatio(1f), contentAlignment = Alignment.Center
            ) {
                Text(firstDayOfWeek.plus(i.toLong()).getDisplayName(TextStyle.SHORT, locale))
            }
        }
    }
}

@Composable
private fun MonthMatrix(
    yearMonth: YearMonth,
    firstDayOfWeek: DayOfWeek,
    selectedDate: LocalDate,
    eventDays: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    dateCellModifier: Modifier = Modifier
) {
    Column(modifier) {
        for (week in buildDateMatrixOfMonth(yearMonth, firstDayOfWeek)) {
            Row {
                for (day in week) {
                    DateCell(
                        day,
                        day == selectedDate,
                        day.month == yearMonth.month,
                        eventDays.contains(day),
                        { onDateClick(day) },
                        modifier = dateCellModifier
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekList(
    firstDay: LocalDate,
    selectedDate: LocalDate,
    eventDays: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    dateCellModifier: Modifier = Modifier
) {
    Row(modifier) {
        for (date in buildDateListOfWeek(firstDay)) {
            DateCell(
                date,
                date == selectedDate,
                true,
                eventDays.contains(date),
                { onDateClick(date) },
                modifier = dateCellModifier
            )
        }
    }
}

@Composable
private fun DayInfo(
    date: LocalDate,
    hasEvent: Boolean,
    modifier: Modifier = Modifier,
    dateCellModifier: Modifier = Modifier
) {
    Row(modifier) {
        PlaceholderCell(3, modifier = dateCellModifier)
        DateCell(
            date, isSelected = true, isPrimary = true, hasEvent, {}, modifier = dateCellModifier
        )
        PlaceholderCell(3, modifier = dateCellModifier)
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    isSelected: Boolean,
    isPrimary: Boolean,
    hasEvent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier, contentAlignment = Alignment.Center
    ) {
        val clickableBoxModifier =
            Modifier
                .fillMaxSize(0.65f)
                .clip(CircleShape)
                .clickable(onClick = onClick)

        Box(
            when {
                isSelected -> clickableBoxModifier.background(MaterialTheme.colorScheme.primary)
                else -> clickableBoxModifier
            }
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(), color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isPrimary -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                }, fontSize = 16.sp, fontWeight = when {
                    isSelected -> FontWeight.Bold
                    else -> FontWeight.Normal
                }
            )
            if (hasEvent) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary.copy(
                                    alpha = 0.6f
                                )

                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun PlaceholderCell(count: Int, modifier: Modifier = Modifier) {
    repeat(count) { Box(modifier) }
}

private fun buildDateMatrixOfMonth(
    yearMonth: YearMonth, firstDayOfWeek: DayOfWeek
): Array<Array<LocalDate>> {
    val firstOfMonth = yearMonth.atDay(1)
    val start =
        firstOfMonth.minusDays((firstOfMonth.dayOfWeek.value - firstDayOfWeek.value).toLong())
            .minusWeeks(if (firstOfMonth.dayOfWeek == firstDayOfWeek) 1L else 0L)

    val matrix = Array(6) { week ->
        val startOfWeek = start.plusWeeks(week.toLong())
        Array<LocalDate>(7) { day ->
            startOfWeek.plusDays(day.toLong())
        }
    }

    return matrix
}

private fun buildDateListOfWeek(
    firstDateOfWeek: LocalDate
): Array<LocalDate> {
    return Array(7) {
        firstDateOfWeek.plusDays(it.toLong())
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthCalendarPreview() {
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }

    Calendar(
        MONTH,
        DayOfWeek.MONDAY,
        Locale.getDefault(),
        selectedDate,
        setOf(LocalDate.now(), LocalDate.now().plusDays(3)),
        { date -> selectedDate = date },
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
private fun WeekCalendarPreview() {
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }

    Calendar(
        WEEK,
        DayOfWeek.MONDAY,
        Locale.getDefault(),
        selectedDate,
        setOf(LocalDate.now(), LocalDate.now().plusDays(3)),
        { date -> selectedDate = date },
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
private fun DayCalendarPreview() {
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }

    Calendar(
        DAY,
        DayOfWeek.MONDAY,
        Locale.getDefault(),
        selectedDate,
        setOf(LocalDate.now(), LocalDate.now().plusDays(3)),
        { date -> selectedDate = date },
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
private fun CalendarPreview() {
    val locale = Locale.getDefault()

    var calendarType by rememberSaveable { mutableStateOf(MONTH) }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }

    Surface(Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ModeSelectionRow(locale, calendarType, { calendarType = it })
            Spacer(Modifier.height(16.dp))
            Calendar(
                calendarType,
                DayOfWeek.MONDAY,
                locale,
                selectedDate,
                setOf(LocalDate.now(), LocalDate.now().plusDays(3)),
                { date -> selectedDate = date },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
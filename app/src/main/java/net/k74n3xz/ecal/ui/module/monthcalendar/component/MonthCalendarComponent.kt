package net.k74n3xz.ecal.ui.module.monthcalendar.component

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.yearMonth
import kotlinx.coroutines.launch
import net.k74n3xz.ecal.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun MonthCalendarComponent(
    busyDates: Set<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // TODO: Define supported calendar bounds centrally instead of hard-coding them in this component.
    val calenderState = rememberCalendarState(
        startMonth = YearMonth.of(1900, 1),
        endMonth = YearMonth.of(2999, 12),
        firstVisibleMonth = YearMonth.now(),
        firstDayOfWeek = firstDayOfWeekFromLocale(),
        outDateStyle = OutDateStyle.EndOfGrid
    )

    HorizontalCalendar(
        modifier = modifier,
        state = calenderState,
        dayContent = {
            Day(
                day = it,
                hasEvent = it.date in busyDates,
                isSelected = it.date == selectedDate,
                onSelected = { day ->
                    if (day.position != DayPosition.MonthDate) {
                        coroutineScope.launch {
                            calenderState.animateScrollToMonth(day.date.yearMonth)
                        }
                    }
                    onDateSelected(day.date)
                }
            )
        },
        monthHeader = {
            Text(
                text = it.yearMonth.format(
                    DateTimeFormatter.ofPattern(
                        DateFormat.getBestDateTimePattern(
                            LocalLocale.current.platformLocale,
                            "yMMMM"
                        ),
                        LocalLocale.current.platformLocale
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )

            val daysOfWeek = remember(firstDayOfWeekFromLocale()) {
                it.weekDays.first().map { day ->
                    day.date.dayOfWeek
                }
            }
            DaysOfWeekTitle(
                daysOfWeek = daysOfWeek,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    )
}

@Composable
private fun Day(
    day: CalendarDay,
    hasEvent: Boolean,
    isSelected: Boolean,
    onSelected: (CalendarDay) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val circleBoxModifier = Modifier
            .fillMaxSize(0.6f)
            .clip(CircleShape)
            .let {
                if (isSelected) {
                    it.background(MaterialTheme.colorScheme.primary.let { bgColor ->
                        when (day.position) {
                            DayPosition.MonthDate -> bgColor
                            else -> bgColor.copy(alpha = 0.4f)
                        }
                    })
                } else {
                    it
                }
            }
            .clickable(
                onClickLabel = stringResource(
                    id = R.string.calendar_day_content_description_select,
                    formatArgs = arrayOf(day.date)
                ),
                onClick = { onSelected(day) }
            )

        Box(
            modifier = circleBoxModifier,
            contentAlignment = Alignment.Center
        ) {
            val textBaseColor = MaterialTheme.colorScheme.let {
                if (isSelected) {
                    it.onPrimary
                } else {
                    it.onSurface
                }
            }

            Column(
                verticalArrangement = Arrangement.SpaceAround,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val dotHeight = 4.dp

                Spacer(modifier = Modifier.height(dotHeight))
                Text(
                    text = day.date.dayOfMonth.toString(),
                    color = when (day.position) {
                        DayPosition.MonthDate -> textBaseColor
                        else -> textBaseColor.copy(alpha = 0.4f)
                    },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (hasEvent) {
                    Box(
                        modifier = Modifier
                            .size(dotHeight)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.let {
                                if (isSelected) {
                                    it.onPrimary
                                } else {
                                    it.primary
                                }
                            }.let {
                                when (day.position) {
                                    DayPosition.MonthDate -> it
                                    else -> it.copy(alpha = 0.4f)
                                }
                            })
                    )
                } else {
                    Spacer(modifier = Modifier.height(dotHeight))
                }
            }
        }
    }
}

@Composable
private fun DaysOfWeekTitle(daysOfWeek: List<DayOfWeek>, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                text = dayOfWeek.getDisplayName(
                    TextStyle.SHORT,
                    LocalLocale.current.platformLocale
                ),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthCalendarComponentPreview() {
    val today = LocalDate.now()

    var selectedDate by remember { mutableStateOf(today) }

    MonthCalendarComponent(
        busyDates = setOf(
            today,
            today.minusDays(2),
            today.plusDays(1),
            today.plusDays(3)
        ),
        selectedDate = selectedDate,
        onDateSelected = { selectedDate = it }
    )
}
package net.k74n3xz.ecal.ui.presentation.form.utils

import java.time.ZoneId
import net.k74n3xz.ecal.core.model.Alarm
import net.k74n3xz.ecal.core.model.Event
import net.k74n3xz.ecal.ui.presentation.form.AlarmForm
import net.k74n3xz.ecal.ui.presentation.form.EventForm

internal fun Event.toEventForm(timeZone: ZoneId): EventForm = EventForm(
    summary,
    description,
    location,
    schedule,
    priority,
    transparency,
    status,
    timeZone
)

internal fun Alarm.toAlarmForm(timeZone: ZoneId): AlarmForm = AlarmForm(
    action,
    trigger,
    repetition,
    timeZone
)

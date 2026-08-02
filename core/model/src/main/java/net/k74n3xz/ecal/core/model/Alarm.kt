package net.k74n3xz.ecal.core.model

import net.k74n3xz.ecal.core.model.property.alarm.Action
import net.k74n3xz.ecal.core.model.property.alarm.Repetition
import net.k74n3xz.ecal.core.model.property.alarm.Trigger

data class Alarm(val id: Long? = null, val action: Action, val trigger: Trigger, val repetition: Repetition? = null)

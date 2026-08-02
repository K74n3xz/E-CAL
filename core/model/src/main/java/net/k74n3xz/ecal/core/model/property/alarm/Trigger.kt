package net.k74n3xz.ecal.core.model.property.alarm

import java.time.Duration
import java.time.Instant

sealed interface Trigger {
    data class RelativeTrigger(val relativeTo: TriggerRelationship = TriggerRelationship.START, val offset: Duration) :
        Trigger

    data class AbsoluteTrigger(val at: Instant) : Trigger
}

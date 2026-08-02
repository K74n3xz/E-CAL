package net.k74n3xz.ecal.core.data.utils

import java.time.ZoneId
import net.k74n3xz.ecal.core.data.database.entity.AlarmEntity
import net.k74n3xz.ecal.core.data.database.entity.AlarmOccurrenceEntity
import net.k74n3xz.ecal.core.data.database.entity.EventEntity
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarm.TriggerType
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.DesiredState
import net.k74n3xz.ecal.core.data.database.entity.enumeration.alarmoccurrence.ReconcileResult
import net.k74n3xz.ecal.core.model.property.alarm.TriggerRelationship
import net.k74n3xz.ecal.core.model.utils.atEndOfDay

internal fun calculateNextAlarmOccurrenceEntity(
    alarmEntity: AlarmEntity,
    // TODO: Generalize reference lookup before alarms can target components other than events.
    referenceEntity: EventEntity,
    lastAlarmOccurrenceIndex: Long?,
    timeZone: ZoneId
): AlarmOccurrenceEntity? {
    require(
        (alarmEntity.interval == null && alarmEntity.repeat == null) ||
            (alarmEntity.interval != null && alarmEntity.repeat != null)
    ) {
        "AlarmEntity(id=${alarmEntity.id}) has incomplete `interval`(=${alarmEntity.interval}) and `repeat`(=${alarmEntity.repeat}). May the record is broken?"
    }

    val firstTriggerTime = when (alarmEntity.triggerType) {
        TriggerType.RELATIVE ->
            when (alarmEntity.triggerRelativeTo!!) {
                TriggerRelationship.START ->
                    referenceEntity.startAt ?: referenceEntity.startDate!!.atStartOfDay(timeZone).toInstant()

                TriggerRelationship.END ->
                    referenceEntity.actualEndAt ?: referenceEntity.actualEndDate!!.atEndOfDay(timeZone).toInstant()
            }.plus(alarmEntity.triggerOffset!!)

        TriggerType.ABSOLUTE -> alarmEntity.triggerAt!!
    }

    return if (lastAlarmOccurrenceIndex == null) {
        AlarmOccurrenceEntity(
            id = null,
            alarmId = alarmEntity.id,
            index = 0,
            triggerAt = firstTriggerTime,
            desiredState = DesiredState.ACTIVE,
            lastReconcileResult = ReconcileResult.CANCELLED
        )
    } else {
        if (alarmEntity.interval != null && alarmEntity.repeat != null) {
            if (lastAlarmOccurrenceIndex < alarmEntity.repeat) {
                AlarmOccurrenceEntity(
                    id = null,
                    alarmId = alarmEntity.id,
                    index = lastAlarmOccurrenceIndex + 1,
                    triggerAt = firstTriggerTime + alarmEntity.interval.multipliedBy(lastAlarmOccurrenceIndex + 1),
                    desiredState = DesiredState.ACTIVE,
                    lastReconcileResult = ReconcileResult.CANCELLED
                )
            } else {
                null
            }
        } else {
            null
        }
    }
}

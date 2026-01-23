package net.k74n3xz.ecal.data.entity.embedded

import net.k74n3xz.ecal.data.entity.enumeration.rrule.Frequency
import net.k74n3xz.ecal.data.entity.enumeration.rrule.RRuleType
import java.time.Instant

data class RecurrenceRule(
    val frequency: Frequency,
    val type: RRuleType,
    val until: Instant? = null,
    val count: Long? = null,
    val interval: Long = 1

    /*
    * TODO: Support the rest of Recurrence Rule (3.3.10) in RFC 5545.
    *   - BYSECOND
    *   - BYMINUTE
    *   - BYHOUR
    *   - BYDAY
    *   - BYMONTHDAY
    *   - BYYEARDAY
    *   - BYWEEKNO
    *   - BYMONTH
    *   - BYSETPOS
    *   - WKST
    * */
)
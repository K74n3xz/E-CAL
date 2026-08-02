package net.k74n3xz.ecal.core.model.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

val UtcZoneId: ZoneId = ZoneId.of("UTC")

fun LocalDate.atEndOfDay(): LocalDateTime = plusDays(1).atStartOfDay().minusNanos(1)

fun LocalDate.atEndOfDay(zone: ZoneId): ZonedDateTime = plusDays(1).atStartOfDay(zone).minusNanos(1)

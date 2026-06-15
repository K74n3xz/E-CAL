package net.k74n3xz.ecal.utils

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

fun LocalDate.atEndOfDay(zone: ZoneId): ZonedDateTime = plusDays(1).atStartOfDay(zone).minusNanos(1)
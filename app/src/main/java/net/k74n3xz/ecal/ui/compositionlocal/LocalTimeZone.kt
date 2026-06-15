package net.k74n3xz.ecal.ui.compositionlocal

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import java.time.ZoneId

val LocalTimeZone: ProvidableCompositionLocal<ZoneId> =
    staticCompositionLocalOf { error("LocalTimeZone is not provided.") }
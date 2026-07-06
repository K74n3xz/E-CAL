package net.k74n3xz.ecal.utils

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun generateEventUid(): String = "${Uuid.generateV7()}-ECAL_event"
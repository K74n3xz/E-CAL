package net.k74n3xz.ecal.ui.module.eventedit.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class EventEdit(val eventUid: String?) : NavKey
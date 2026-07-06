package net.k74n3xz.ecal.ui.module.eventedit.viewmodel.state

import net.k74n3xz.ecal.core.model.Event

sealed interface EditMode {
    data object AddEventMode : EditMode
    data class EditEventMode(val event: Event) : EditMode
}
package net.k74n3xz.ecal.ui.module.eventedit.viewmodel

import net.k74n3xz.ecal.ui.module.eventedit.viewmodel.state.EditMode
import net.k74n3xz.ecal.ui.module.eventedit.viewmodel.state.EditOperationState

data class EventEditUiState(
    val editMode: EditMode?,
    val operationState: EditOperationState
)
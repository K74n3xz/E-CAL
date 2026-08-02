package net.k74n3xz.ecal.ui.presentation.form

import androidx.compose.foundation.text.input.TextFieldState

internal fun TextFieldState.replaceText(value: String) {
    edit { replace(0, length, value) }
}

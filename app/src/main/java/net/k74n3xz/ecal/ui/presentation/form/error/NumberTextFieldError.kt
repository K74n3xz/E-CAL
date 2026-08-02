package net.k74n3xz.ecal.ui.presentation.form.error

internal sealed interface NumberTextFieldError {
    data object Empty : NumberTextFieldError
    data object InvalidCharacter : NumberTextFieldError
    data object TooBigNumber : NumberTextFieldError
}

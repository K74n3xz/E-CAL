package net.k74n3xz.ecal.ui.presentation.form.error

internal sealed interface NumberFieldError {
    data object OutOfRange : NumberFieldError
}

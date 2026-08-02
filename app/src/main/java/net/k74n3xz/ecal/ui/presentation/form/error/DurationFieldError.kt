package net.k74n3xz.ecal.ui.presentation.form.error

internal sealed interface DurationFieldError {
    data object NotPositive : DurationFieldError
}

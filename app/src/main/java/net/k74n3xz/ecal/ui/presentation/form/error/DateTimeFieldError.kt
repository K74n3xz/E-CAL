package net.k74n3xz.ecal.ui.presentation.form.error

internal sealed interface DateTimeFieldError {
    data object TooLongPeriod : DateTimeFieldError
    data object EndBeforeStart : DateTimeFieldError
    data object EndNotAfterStart : DateTimeFieldError
}

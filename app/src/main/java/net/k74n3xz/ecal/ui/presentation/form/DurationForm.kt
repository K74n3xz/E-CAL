package net.k74n3xz.ecal.ui.presentation.form

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.text.isDigitsOnly
import java.time.Duration
import net.k74n3xz.ecal.ui.presentation.form.enumeration.common.Sign
import net.k74n3xz.ecal.ui.presentation.form.error.NumberTextFieldError

@Stable
internal class DurationForm(initialDuration: Duration) {
    private val absoluteInitialDuration: Duration = initialDuration.abs()

    var sign: Sign by mutableStateOf(if (initialDuration.isNegative) Sign.NEGATIVE else Sign.POSITIVE)

    val daysField: TextFieldState = TextFieldState(absoluteInitialDuration.toDaysPart().toString())
    val daysFieldError: NumberTextFieldError?
        get() = validateField(daysField.text.toString())

    val hoursField: TextFieldState = TextFieldState(absoluteInitialDuration.toHoursPart().toString())
    val hoursFieldError: NumberTextFieldError?
        get() = validateField(hoursField.text.toString())

    val minutesField: TextFieldState = TextFieldState(absoluteInitialDuration.toMinutesPart().toString())
    val minutesFieldError: NumberTextFieldError?
        get() = validateField(minutesField.text.toString())

    val secondsField: TextFieldState = TextFieldState(absoluteInitialDuration.toSecondsPart().toString())
    val secondsFieldError: NumberTextFieldError?
        get() = validateField(secondsField.text.toString())

    val millisField: TextFieldState = TextFieldState(absoluteInitialDuration.toMillisPart().toString())
    val millisFieldError: NumberTextFieldError?
        get() = validateField(millisField.text.toString())

    val nanosField: TextFieldState = TextFieldState((absoluteInitialDuration.toNanosPart() % 1_000_000).toString())
    val nanosFieldError: NumberTextFieldError?
        get() = validateField(nanosField.text.toString())

    val isValid: Boolean
        get() = daysFieldError == null &&
            hoursFieldError == null &&
            minutesFieldError == null &&
            secondsFieldError == null &&
            millisFieldError == null &&
            nanosFieldError == null

    private fun validateField(text: String): NumberTextFieldError? = if (!text.isDigitsOnly()) {
        NumberTextFieldError.InvalidCharacter
    } else if (text.isNotEmpty() && text.toLongOrNull() == null) {
        NumberTextFieldError.TooBigNumber
    } else {
        null
    }

    private fun resolvePart(text: String): Long = if (text.isEmpty()) {
        0
    } else {
        text.toLong()
    }

    fun resolve(): Result<Duration> = if (isValid) {
        try {
            val days = resolvePart(daysField.text.toString())
            val hours = resolvePart(hoursField.text.toString())
            val minutes = resolvePart(minutesField.text.toString())
            val seconds = resolvePart(secondsField.text.toString())
            val millis = resolvePart(millisField.text.toString())
            val nanos = resolvePart(nanosField.text.toString())

            Result.success(
                Duration.ZERO
                    .plusDays(days)
                    .plusHours(hours)
                    .plusMinutes(minutes)
                    .plusSeconds(seconds)
                    .plusMillis(millis)
                    .plusNanos(nanos)
                    .let {
                        when (sign) {
                            Sign.NEGATIVE -> it.negated()
                            Sign.POSITIVE -> it
                        }
                    }
            )
        } catch (numberFormatException: NumberFormatException) {
            Result.failure(numberFormatException)
        } catch (arithmeticException: ArithmeticException) {
            Result.failure(arithmeticException)
        }
    } else {
        Result.failure(IllegalArgumentException("Unresolvable input."))
    }
}

package net.k74n3xz.ecal.ui.presentation.form

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.text.isDigitsOnly
import java.time.Period
import kotlin.math.absoluteValue
import net.k74n3xz.ecal.ui.presentation.form.enumeration.common.Sign
import net.k74n3xz.ecal.ui.presentation.form.error.NumberTextFieldError

@Stable
internal class PeriodForm(initialPeriod: Period) {
    var yearMonthSign: Sign by mutableStateOf(if (initialPeriod.toTotalMonths() < 0) Sign.NEGATIVE else Sign.POSITIVE)

    val yearsField: TextFieldState = TextFieldState(initialPeriod.normalized().years.absoluteValue.toString())
    val yearsFieldError: NumberTextFieldError?
        get() = validateField(yearsField.text.toString())

    val monthsField: TextFieldState = TextFieldState(initialPeriod.normalized().months.absoluteValue.toString())
    val monthsFieldError: NumberTextFieldError?
        get() = validateField(monthsField.text.toString())

    var daySign: Sign by mutableStateOf(if (initialPeriod.days < 0) Sign.NEGATIVE else Sign.POSITIVE)

    val daysField: TextFieldState = TextFieldState(initialPeriod.days.absoluteValue.toString())
    val daysFieldError: NumberTextFieldError?
        get() = validateField(daysField.text.toString())

    val isValid: Boolean
        get() = yearsFieldError == null && monthsFieldError == null && daysFieldError == null

    private fun validateField(text: String): NumberTextFieldError? = if (!text.isDigitsOnly()) {
        NumberTextFieldError.InvalidCharacter
    } else if (text.isNotEmpty() && text.toIntOrNull() == null) {
        NumberTextFieldError.TooBigNumber
    } else {
        null
    }

    private fun resolvePart(text: String): Int = if (text.isEmpty()) {
        0
    } else {
        text.toInt()
    }

    fun resolve(): Result<Period> = if (isValid) {
        try {
            val years = resolvePart(yearsField.text.toString()) * if (yearMonthSign == Sign.POSITIVE) 1 else -1
            val months = resolvePart(monthsField.text.toString()) * if (yearMonthSign == Sign.POSITIVE) 1 else -1
            val days = resolvePart(daysField.text.toString()) * if (daySign == Sign.POSITIVE) 1 else -1

            Result.success(Period.of(years, months, days).normalized())
        } catch (numberFormatException: NumberFormatException) {
            Result.failure(numberFormatException)
        } catch (arithmeticException: ArithmeticException) {
            Result.failure(arithmeticException)
        }
    } else {
        Result.failure(IllegalArgumentException("Unresolvable input."))
    }
}

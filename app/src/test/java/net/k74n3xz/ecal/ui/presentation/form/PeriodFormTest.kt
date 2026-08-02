package net.k74n3xz.ecal.ui.presentation.form

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Period
import net.k74n3xz.ecal.ui.presentation.form.enumeration.common.Sign
import net.k74n3xz.ecal.ui.presentation.form.error.NumberTextFieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Config.TARGET_SDK], application = Application::class)
class PeriodFormTest {
    @Test
    fun initialization_normalizesMixedYearMonthSignsAndKeepsDaySign() {
        val positiveMonths = PeriodForm(Period.of(1, -1, -2))
        val negativeMonths = PeriodForm(Period.of(-1, 1, 2))

        assertEquals(Sign.POSITIVE, positiveMonths.yearMonthSign)
        assertEquals("0", positiveMonths.yearsField.text.toString())
        assertEquals("11", positiveMonths.monthsField.text.toString())
        assertEquals(Sign.NEGATIVE, positiveMonths.daySign)
        assertEquals("2", positiveMonths.daysField.text.toString())
        assertEquals(Period.of(0, 11, -2), positiveMonths.resolve().getOrThrow())

        assertEquals(Sign.NEGATIVE, negativeMonths.yearMonthSign)
        assertEquals("0", negativeMonths.yearsField.text.toString())
        assertEquals("11", negativeMonths.monthsField.text.toString())
        assertEquals(Sign.POSITIVE, negativeMonths.daySign)
        assertEquals("2", negativeMonths.daysField.text.toString())
        assertEquals(Period.of(0, -11, 2), negativeMonths.resolve().getOrThrow())
    }

    @Test
    fun resolve_combinesEditedPartsAndAppliesIndependentSigns() {
        val form = PeriodForm(Period.ZERO).apply {
            yearsField.replaceText("1")
            monthsField.replaceText("2")
            daysField.replaceText("3")
            yearMonthSign = Sign.NEGATIVE
            daySign = Sign.POSITIVE
        }

        assertEquals(Period.of(-1, -2, 3), form.resolve().getOrThrow())
    }

    @Test
    fun emptyParts_areValidAndResolveAsZero() {
        val form = PeriodForm(Period.ofDays(1))
        listOf(form.yearsField, form.monthsField, form.daysField).forEach { it.replaceText("") }

        assertTrue(form.isValid)
        assertEquals(Period.ZERO, form.resolve().getOrThrow())
    }

    @Test
    fun invalidCharactersAndTooLargeNumbers_areRejected() {
        val form = PeriodForm(Period.ZERO)

        form.monthsField.replaceText("1x")
        assertEquals(NumberTextFieldError.InvalidCharacter, form.monthsFieldError)
        assertFalse(form.isValid)
        assertTrue(form.resolve().isFailure)

        form.monthsField.replaceText("2147483648")
        assertEquals(NumberTextFieldError.TooBigNumber, form.monthsFieldError)
    }

    @Test
    fun normalizationOverflow_isReturnedAsFailure() {
        val form = PeriodForm(Period.ZERO).apply {
            yearsField.replaceText(Int.MAX_VALUE.toString())
            monthsField.replaceText(Int.MAX_VALUE.toString())
        }

        assertTrue(form.isValid)
        assertTrue(form.resolve().isFailure)
    }
}

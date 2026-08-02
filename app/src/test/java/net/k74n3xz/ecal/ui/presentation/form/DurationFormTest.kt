package net.k74n3xz.ecal.ui.presentation.form

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
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
class DurationFormTest {
    @Test
    fun initialization_decomposesAbsoluteDurationAndKeepsSign() {
        val duration = Duration.ofDays(1)
            .plusHours(2)
            .plusMinutes(3)
            .plusSeconds(4)
            .plusMillis(5)
            .plusNanos(6)

        val positive = DurationForm(duration)
        val negative = DurationForm(duration.negated())

        listOf(positive, negative).forEach { form ->
            assertEquals("1", form.daysField.text.toString())
            assertEquals("2", form.hoursField.text.toString())
            assertEquals("3", form.minutesField.text.toString())
            assertEquals("4", form.secondsField.text.toString())
            assertEquals("5", form.millisField.text.toString())
            assertEquals("6", form.nanosField.text.toString())
        }
        assertEquals(Sign.POSITIVE, positive.sign)
        assertEquals(Sign.NEGATIVE, negative.sign)
    }

    @Test
    fun resolve_combinesEditedPartsAndAppliesSign() {
        val form = DurationForm(Duration.ZERO).apply {
            daysField.replaceText("1")
            hoursField.replaceText("25")
            minutesField.replaceText("2")
            secondsField.replaceText("3")
            millisField.replaceText("4")
            nanosField.replaceText("5")
            sign = Sign.NEGATIVE
        }

        assertEquals(
            Duration.ofDays(1).plusHours(25).plusMinutes(2).plusSeconds(3).plusMillis(4).plusNanos(5).negated(),
            form.resolve().getOrThrow()
        )
    }

    @Test
    fun emptyParts_areValidAndResolveAsZero() {
        val form = DurationForm(Duration.ofMinutes(1))
        listOf(
            form.daysField,
            form.hoursField,
            form.minutesField,
            form.secondsField,
            form.millisField,
            form.nanosField
        ).forEach { it.replaceText("") }

        assertTrue(form.isValid)
        assertEquals(Duration.ZERO, form.resolve().getOrThrow())
    }

    @Test
    fun invalidCharactersAndTooLargeNumbers_areRejected() {
        val form = DurationForm(Duration.ZERO)

        form.minutesField.replaceText("1x")
        assertEquals(NumberTextFieldError.InvalidCharacter, form.minutesFieldError)
        assertFalse(form.isValid)
        assertTrue(form.resolve().isFailure)

        form.minutesField.replaceText("9223372036854775808")
        assertEquals(NumberTextFieldError.TooBigNumber, form.minutesFieldError)
    }

    @Test
    fun arithmeticOverflow_isReturnedAsFailure() {
        val form = DurationForm(Duration.ZERO)
        form.daysField.replaceText(Long.MAX_VALUE.toString())

        assertTrue(form.isValid)
        assertTrue(form.resolve().isFailure)
    }
}

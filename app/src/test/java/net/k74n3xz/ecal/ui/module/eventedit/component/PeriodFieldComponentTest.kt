package net.k74n3xz.ecal.ui.module.eventedit.component

import android.app.Application
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Period
import net.k74n3xz.ecal.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Config.TARGET_SDK], application = Application::class)
class PeriodFieldComponentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun saveWithoutEdits_normalizesMixedYearMonthSignsWithoutChangingMeaning() {
        var savedPeriod: Period? = null
        composeRule.setContent {
            MaterialTheme {
                PeriodFieldComponent(
                    period = Period.of(1, -1, 0),
                    onSave = { savedPeriod = it }
                )
            }
        }

        composeRule.onNodeWithContentDescription(resource(R.string.button_content_description_edit)).performClick()
        composeRule.onNodeWithText(resource(R.string.text_save)).performClick()

        composeRule.runOnIdle {
            assertEquals(Period.ofMonths(11), savedPeriod)
        }
    }

    private fun resource(id: Int): String = ApplicationProvider.getApplicationContext<Context>().getString(id)
}

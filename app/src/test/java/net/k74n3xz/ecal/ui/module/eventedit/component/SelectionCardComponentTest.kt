package net.k74n3xz.ecal.ui.module.eventedit.component

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.core.model.Attendee
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Config.TARGET_SDK], application = Application::class)
class SelectionCardComponentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun attendeeCard_displaysDetailsAndReportsSelection() {
        val attendee = Attendee(5, "User", "Organizer", "user@example.com")
        var selected: Boolean? = null
        composeRule.setContent {
            MaterialTheme {
                AttendeeSelectionCard(attendee, isSelected = false, onSelectedChange = { selected = it })
            }
        }

        composeRule.onNodeWithText("User").assertExists()
        composeRule.onNodeWithText("Organizer").assertExists()
        composeRule.onNodeWithText("user@example.com").assertExists()
        composeRule.onNode(isToggleable()).assertIsOff().performClick()

        composeRule.runOnIdle { assertEquals(true, selected) }
    }

    @Test
    fun attachmentCard_displaysMetadataAndReportsDeselection() {
        val attachment = Attachment(7, "Agenda", "agenda.pdf", "application/pdf", 2048)
        var selected: Boolean? = null
        composeRule.setContent {
            MaterialTheme {
                AttachmentSelectionCard(attachment, isSelected = true, onSelectedChange = { selected = it })
            }
        }

        composeRule.onNodeWithText("agenda.pdf").assertExists()
        composeRule.onNodeWithText("Agenda").assertExists()
        composeRule.onNodeWithText("application/pdf", substring = true).assertExists()
        composeRule.onNode(isToggleable()).assertIsOn().performClick()

        composeRule.runOnIdle { assertEquals(false, selected) }
    }
}

package net.k74n3xz.ecal.ui.module.attendeemanagement

import android.app.Application
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.core.model.Attendee
import net.k74n3xz.ecal.ui.module.attendeemanagement.component.AttendeeDeletionConfirmationDialog
import net.k74n3xz.ecal.ui.module.attendeemanagement.component.AttendeeEditDialog
import net.k74n3xz.ecal.ui.module.attendeemanagement.screen.AttendeeManagementScreen
import net.k74n3xz.ecal.ui.module.monthcalendar.component.scaffold.MonthCalendarTopBarComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Config.TARGET_SDK], application = Application::class)
class AttendeeManagementComponentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyList_showsLocalizedEmptyState() {
        composeRule.setContent {
            MaterialTheme {
                AttendeeManagementScreen(emptyList(), true, {}, {})
            }
        }

        composeRule.onNodeWithText(resource(R.string.text_no_attendees)).assertIsDisplayed()
    }

    @Test
    fun attendeeCard_displaysFieldsAndRoutesActions() {
        val attendee = attendee()
        var edited: Attendee? = null
        var deleted: Attendee? = null
        composeRule.setContent {
            MaterialTheme {
                AttendeeManagementScreen(
                    attendees = listOf(attendee),
                    operationsEnabled = true,
                    onEdit = { edited = it },
                    onDelete = { deleted = it }
                )
            }
        }

        composeRule.onNodeWithText(attendee.name!!).assertIsDisplayed()
        composeRule.onNodeWithText(attendee.email).assertIsDisplayed()
        composeRule.onNodeWithText(attendee.description!!).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            resource(R.string.button_content_description_edit_attendee, attendee.name!!)
        ).performClick()
        composeRule.onNodeWithContentDescription(
            resource(R.string.button_content_description_delete_attendee, attendee.name!!)
        ).performClick()

        composeRule.runOnIdle {
            assertEquals(attendee, edited)
            assertEquals(attendee, deleted)
        }
    }

    @Test
    fun disabledOperations_disableCardActions() {
        val attendee = attendee()
        composeRule.setContent {
            MaterialTheme { AttendeeManagementScreen(listOf(attendee), false, {}, {}) }
        }

        composeRule.onNodeWithContentDescription(
            resource(R.string.button_content_description_edit_attendee, attendee.name!!)
        ).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(
            resource(R.string.button_content_description_delete_attendee, attendee.name!!)
        ).assertIsNotEnabled()
    }

    @Test
    fun addDialog_requiresValidEmailAndPreservesOptionalFieldInput() {
        var confirmed: Attendee? = null
        composeRule.setContent {
            MaterialTheme {
                AttendeeEditDialog(null, {}, { confirmed = it })
            }
        }

        composeRule.onNodeWithText(resource(R.string.text_add)).assertIsNotEnabled()
        composeRule.onNodeWithText(resource(R.string.text_field_label_name))
            .performTextReplacement("  Ada  ")
        composeRule.onNodeWithText(resource(R.string.text_field_label_description))
            .performTextReplacement("   ")
        composeRule.onNodeWithText(resource(R.string.text_field_label_email))
            .performTextReplacement("invalid")
        composeRule.onNodeWithText(resource(R.string.error_invalid_email)).assertIsDisplayed()
        composeRule.onNodeWithText(resource(R.string.text_add)).assertIsNotEnabled()

        composeRule.onNodeWithText("invalid").performTextReplacement("  ada@example.com  ")
        composeRule.onNodeWithText(resource(R.string.text_add)).assertIsEnabled().performClick()

        composeRule.runOnIdle {
            assertEquals(Attendee(name = "  Ada  ", description = "   ", email = "ada@example.com"), confirmed)
        }
    }

    @Test
    fun editDialog_preservesUntouchedNullOptionalFields() {
        val attendee = Attendee(id = 7, name = null, description = null, email = "ada@example.com")
        var confirmed: Attendee? = null
        composeRule.setContent {
            MaterialTheme { AttendeeEditDialog(attendee, {}, { confirmed = it }) }
        }

        composeRule.onNodeWithText(resource(R.string.text_save)).performClick()

        composeRule.runOnIdle { assertEquals(attendee, confirmed) }
    }

    @Test
    fun editDialog_explicitClearConvertsOptionalFieldsToNull() {
        val attendee = attendee()
        var confirmed: Attendee? = null
        composeRule.setContent {
            MaterialTheme { AttendeeEditDialog(attendee, {}, { confirmed = it }) }
        }

        val clearDescription = resource(R.string.button_content_description_clear)
        composeRule.onAllNodesWithContentDescription(clearDescription)[0].performClick()
        composeRule.onAllNodesWithContentDescription(clearDescription)[0].performClick()
        composeRule.onNodeWithText(resource(R.string.text_save)).performClick()

        composeRule.runOnIdle {
            assertEquals(attendee.copy(name = null, description = null), confirmed)
        }
    }

    @Test
    fun editDialog_preservesIdAndPrefillsFields() {
        val attendee = attendee()
        var confirmed: Attendee? = null
        composeRule.setContent {
            MaterialTheme { AttendeeEditDialog(attendee, {}, { confirmed = it }) }
        }

        composeRule.onNodeWithText(attendee.name!!).assertIsDisplayed()
        composeRule.onNodeWithText(resource(R.string.text_save)).performClick()

        composeRule.runOnIdle { assertEquals(attendee, confirmed) }
    }

    @Test
    fun deletionDialog_requiresExplicitConfirmation() {
        var confirmed = false
        var dismissed = false
        composeRule.setContent {
            MaterialTheme {
                AttendeeDeletionConfirmationDialog(
                    attendeeName = "Ada",
                    onDismiss = { dismissed = true },
                    onConfirm = { confirmed = true }
                )
            }
        }

        composeRule.onNodeWithText(resource(R.string.text_cancel)).performClick()
        composeRule.runOnIdle {
            assertTrue(dismissed)
            assertFalse(confirmed)
        }
        composeRule.onNodeWithText(resource(R.string.text_delete)).performClick()
        composeRule.runOnIdle { assertTrue(confirmed) }
    }

    @Test
    fun monthTopBar_routesBothManagementActions() {
        var attendeeCalls = 0
        var attachmentCalls = 0
        composeRule.setContent {
            MaterialTheme {
                MonthCalendarTopBarComponent(
                    onManageAttendees = { attendeeCalls++ },
                    onManageAttachments = { attachmentCalls++ }
                )
            }
        }

        composeRule.onNodeWithContentDescription(resource(R.string.button_content_description_manage_attendees))
            .performClick()
        composeRule.onNodeWithContentDescription(resource(R.string.button_content_description_manage_attachments))
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, attendeeCalls)
            assertEquals(1, attachmentCalls)
        }
    }

    private fun resource(id: Int, vararg arguments: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *arguments)

    private fun attendee() = Attendee(7, "Ada", "Organizer", "ada@example.com")
}

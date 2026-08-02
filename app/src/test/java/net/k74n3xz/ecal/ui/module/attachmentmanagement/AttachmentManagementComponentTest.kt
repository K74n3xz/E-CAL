package net.k74n3xz.ecal.ui.module.attachmentmanagement

import android.app.Application
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.k74n3xz.ecal.R
import net.k74n3xz.ecal.core.model.Attachment
import net.k74n3xz.ecal.ui.module.attachmentmanagement.component.AttachmentDeletionConfirmationDialog
import net.k74n3xz.ecal.ui.module.attachmentmanagement.component.AttachmentEditDialog
import net.k74n3xz.ecal.ui.module.attachmentmanagement.screen.AttachmentManagementScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Config.TARGET_SDK], application = Application::class)
class AttachmentManagementComponentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyList_showsLocalizedEmptyState() {
        composeRule.setContent {
            MaterialTheme {
                AttachmentManagementScreen(
                    attachments = emptyList(),
                    operationsEnabled = true,
                    onEdit = {},
                    onDelete = {}
                )
            }
        }

        composeRule.onNodeWithText(resource(R.string.text_no_attachments)).assertIsDisplayed()
    }

    @Test
    fun attachmentCard_displaysMetadataAndRoutesActions() {
        val attachment = attachment()
        var edited: Attachment? = null
        var deleted: Attachment? = null
        composeRule.setContent {
            MaterialTheme {
                AttachmentManagementScreen(
                    attachments = listOf(attachment),
                    operationsEnabled = true,
                    onEdit = { edited = it },
                    onDelete = { deleted = it }
                )
            }
        }

        composeRule.onNodeWithText(attachment.name).assertIsDisplayed()
        composeRule.onNodeWithText(attachment.description!!).assertIsDisplayed()
        composeRule.onNodeWithText(attachment.mimeType, substring = true).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(
            resource(R.string.button_content_description_edit_attachment, attachment.name)
        ).performClick()
        composeRule.onNodeWithContentDescription(
            resource(R.string.button_content_description_delete_attachment, attachment.name)
        ).performClick()

        composeRule.runOnIdle {
            assertEquals(attachment, edited)
            assertEquals(attachment, deleted)
        }
    }

    @Test
    fun disabledOperations_leaveCardVisibleButDisableActions() {
        val attachment = attachment()
        composeRule.setContent {
            MaterialTheme {
                AttachmentManagementScreen(
                    attachments = listOf(attachment),
                    operationsEnabled = false,
                    onEdit = {},
                    onDelete = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            resource(R.string.button_content_description_edit_attachment, attachment.name)
        ).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(
            resource(R.string.button_content_description_delete_attachment, attachment.name)
        ).assertIsNotEnabled()
    }

    @Test
    fun addDialog_requiresASelectedDocumentAndSupportsCancel() {
        var dismissed = false
        composeRule.setContent {
            MaterialTheme {
                AttachmentEditDialog(
                    initialDescription = null,
                    canImport = true,
                    onDismiss = { dismissed = true },
                    onConfirm = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText(resource(R.string.text_field_label_description))
            .performTextReplacement("Agenda")
        composeRule.onNodeWithText(resource(R.string.text_add)).assertIsNotEnabled()
        composeRule.onNodeWithText(resource(R.string.text_select_file)).assertIsDisplayed()
        composeRule.onNodeWithText(resource(R.string.text_cancel)).performClick()
        composeRule.runOnIdle { assertTrue(dismissed) }
    }

    @Test
    fun editDescriptionDialog_prefillsAndForwardsEditsAndClearSemantics() {
        var confirmed: String? = "unchanged"
        composeRule.setContent {
            MaterialTheme {
                AttachmentEditDialog(
                    initialDescription = "Agenda",
                    canImport = false,
                    onDismiss = {},
                    onConfirm = { description, _ -> confirmed = description }
                )
            }
        }

        composeRule.onNodeWithText("Agenda").performTextReplacement("Updated agenda")
        composeRule.onNodeWithText(resource(R.string.text_save)).performClick()
        composeRule.runOnIdle { assertEquals("Updated agenda", confirmed) }

        composeRule.onNodeWithContentDescription(resource(R.string.button_content_description_clear))
            .performClick()
        composeRule.onNodeWithText(resource(R.string.text_save)).performClick()

        composeRule.runOnIdle { assertNull(confirmed) }
    }

    @Test
    fun deletionDialog_requiresExplicitConfirmation() {
        var confirmed = false
        var dismissed = false
        composeRule.setContent {
            MaterialTheme {
                AttachmentDeletionConfirmationDialog(
                    attachmentName = "agenda.pdf",
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

    private fun resource(id: Int, vararg arguments: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *arguments)

    private fun attachment() = Attachment(
        id = 7,
        description = "Project agenda",
        name = "agenda.pdf",
        mimeType = "application/pdf",
        sizeBytes = 2048
    )
}

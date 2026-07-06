package net.k74n3xz.ecal.ui.module.eventedit.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ColorOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun disabled_clickReachesContent() {
        var clicks = 0
        composeRule.setContent {
            ColorOverlay(
                enabled = false,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(ROOT_TAG)
            ) {
                Button(onClick = { clicks++ }, modifier = Modifier.fillMaxSize()) {
                    Text("Content")
                }
            }
        }

        composeRule.onNodeWithTag(ROOT_TAG).performTouchInput { click() }

        composeRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun enabled_clickIsConsumedByOverlay() {
        var clicks = 0
        composeRule.setContent {
            ColorOverlay(
                enabled = true,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(ROOT_TAG)
            ) {
                Button(onClick = { clicks++ }, modifier = Modifier.fillMaxSize()) {
                    Text("Content")
                }
            }
        }

        composeRule.onNodeWithTag(ROOT_TAG).performTouchInput { click() }

        composeRule.runOnIdle { assertEquals(0, clicks) }
    }

    @Test
    fun enabled_displaysOverlayContent() {
        composeRule.setContent {
            ColorOverlay(
                enabled = true,
                modifier = Modifier.fillMaxSize(),
                contentOnOverlay = { Text("Overlay content") }
            ) {
                Text("Content")
            }
        }

        composeRule.onNodeWithText("Overlay content").assertIsDisplayed()
    }

    private companion object {
        const val ROOT_TAG = "color-overlay-root"
    }
}
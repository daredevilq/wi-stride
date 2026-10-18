package io.github.daredevilq.wistride.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WiStrideAppTest {
    @get:Rule
    val rule = createComposeRule()

    private fun tab(label: String) = hasText(label) and hasClickAction()

    // Top bar title + screen title are the two non-clickable nodes showing the label.
    private fun assertScreenShown(label: String) {
        rule.onAllNodes(hasText(label) and !hasClickAction()).assertCountEquals(2)
    }

    @Test
    fun clickingEachTabShowsItsScreen() {
        rule.setContent { WiStrideTheme { WiStrideApp() } }
        listOf("Now", "Ping", "Networks", "Speed", "Map").forEach { label ->
            rule.onNode(tab(label)).performClick()
            rule.onNode(tab(label) and isSelected()).assertIsSelected()
            assertScreenShown(label)
        }
    }

    @Test
    fun settingsOpensAndBackReturns() {
        rule.setContent { WiStrideTheme { WiStrideApp() } }
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Settings").performClick()
        rule.onAllNodesWithText("Settings").assertCountEquals(2)
        rule.onNodeWithContentDescription("Navigate up").performClick()
        rule.onNodeWithContentDescription("More options").assertExists()
        assertScreenShown("Now")
    }

    @Test
    fun selectedTabSurvivesStateRestoration() {
        val restorer = StateRestorationTester(rule)
        restorer.setContent { WiStrideTheme { WiStrideApp() } }
        rule.onNode(tab("Speed")).performClick()
        restorer.emulateSavedInstanceStateRestore()
        rule.onNode(tab("Speed") and isSelected()).assertIsSelected()
        assertScreenShown("Speed")
    }
}

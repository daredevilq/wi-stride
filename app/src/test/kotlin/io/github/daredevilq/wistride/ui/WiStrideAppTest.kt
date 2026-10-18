package io.github.daredevilq.wistride.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.daredevilq.wistride.HiltTestActivity
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class WiStrideAppTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun setUp() = hiltRule.inject()

    private fun tab(label: String) = hasText(label) and hasClickAction()

    // Top bar title + screen title are the two non-clickable nodes showing the label.
    private fun assertScreenShown(label: String) {
        rule.onAllNodes(hasText(label) and !hasClickAction()).assertCountEquals(2)
    }

    // The Now screen has no title of its own, and which state it settles in depends on the
    // Robolectric Wi-Fi shadow, so accept any of its stable top-level states.
    private fun assertNowShown() {
        val shown =
            listOf(
                "Reading Wi-Fi status",
                "Wi-Fi is off",
                "Not connected to Wi-Fi",
                "Airplane mode is on",
                "Signal over the last 60 seconds",
            ).map { hasText(it, substring = true) }.reduce { acc, m -> acc or m }
        check(rule.onAllNodes(shown).fetchSemanticsNodes().isNotEmpty()) { "Now screen not shown" }
    }

    // Ping shows its actions instead of a title; which state it settles in depends on the shadows.
    private fun assertPingShown() {
        val shown = hasText("Preparing measurements", substring = true) or hasText("Add target")
        check(rule.onAllNodes(shown).fetchSemanticsNodes().isNotEmpty()) { "Ping screen not shown" }
    }

    @Test
    fun clickingEachTabShowsItsScreen() {
        rule.setContent { WiStrideTheme { WiStrideApp() } }
        listOf("Now", "Ping", "Networks", "Speed", "Map").forEach { label ->
            rule.onNode(tab(label)).performClick()
            rule.onNode(tab(label) and isSelected()).assertIsSelected()
            when (label) {
                "Now" -> assertNowShown()
                "Ping" -> assertPingShown()
                else -> assertScreenShown(label)
            }
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
        assertNowShown()
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

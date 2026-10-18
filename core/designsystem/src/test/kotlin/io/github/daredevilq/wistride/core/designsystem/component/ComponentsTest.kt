package io.github.daredevilq.wistride.core.designsystem.component

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.daredevilq.wistride.core.designsystem.theme.WiStrideTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun valueRowShowsValue() {
        rule.setContent { WiStrideTheme { ValueRow(label = "SSID", value = "HomeNet") } }
        rule.onNodeWithText("SSID").assertIsDisplayed()
        rule.onNodeWithText("HomeNet").assertIsDisplayed()
    }

    @Test
    fun valueRowNullShowsUnavailableAndReason() {
        rule.setContent {
            WiStrideTheme { ValueRow(label = "Speed", value = null, unavailableReason = "Needs Android 11") }
        }
        rule.onNodeWithText("Unavailable").assertIsDisplayed()
        rule.onNodeWithText("Needs Android 11").assertIsDisplayed()
    }

    @Test
    fun valueRowLongPressInvokesCallbackAndExposesAccessibilityAction() {
        var copies = 0
        rule.setContent {
            WiStrideTheme {
                ValueRow(label = "SSID", value = "HomeNet", onLongClick = { copies++ }, onLongClickLabel = "Copy")
            }
        }
        rule.onNodeWithText("HomeNet").performTouchInput { longClick() }
        assertEquals(1, copies)
        rule.onNode(hasText("HomeNet")).performSemanticsAction(SemanticsActions.OnLongClick)
        assertEquals(2, copies)
    }

    @Test
    fun valueRowWithoutLongClickHasNoLongClickAction() {
        rule.setContent { WiStrideTheme { ValueRow(label = "SSID", value = "HomeNet") } }
        rule.onNodeWithText("HomeNet").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnLongClick))
    }

    @Test
    fun statusCardActionInvokesCallback() {
        var clicks = 0
        rule.setContent {
            WiStrideTheme {
                StatusCard(title = "T", message = "M", actionLabel = "Fix", onAction = { clicks++ })
            }
        }
        rule.onNodeWithText("Fix").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun statusCardWithoutCallbackHasNoAction() {
        rule.setContent {
            WiStrideTheme { StatusCard(title = "T", message = "M", actionLabel = "Fix", onAction = null) }
        }
        rule.onNodeWithText("M").assertIsDisplayed()
        rule.onNodeWithText("Fix").assertDoesNotExist()
    }
}

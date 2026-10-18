package io.github.daredevilq.wistride.core.ui.permission

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.github.daredevilq.wistride.core.model.permission.PermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PermissionRationaleCardTest {
    @get:Rule
    val rule = createComposeRule()

    private var requests = 0
    private var settingsOpens = 0

    private fun setCard(status: PermissionStatus) {
        rule.setContent {
            PermissionRationaleCard(
                title = "Location",
                rationale = "Needed to read the Wi-Fi name.",
                status = status,
                onRequest = { requests++ },
                onOpenSettings = { settingsOpens++ },
            )
        }
    }

    @Test
    fun notRequestedShowsAllowAndFiresRequest() {
        setCard(PermissionStatus.NotRequested)
        rule.onNodeWithText("Needed to read the Wi-Fi name.").assertExists()
        rule
            .onNodeWithText("Allow")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        rule.onNodeWithText("Open settings").assertDoesNotExist()
        assertEquals(1, requests)
        assertEquals(0, settingsOpens)
    }

    @Test
    fun deniedShowsAllowAndFiresRequest() {
        setCard(PermissionStatus.Denied)
        rule.onNodeWithText("Allow").performClick()
        assertEquals(1, requests)
    }

    @Test
    fun permanentlyDeniedShowsExplanationAndOpenSettings() {
        setCard(PermissionStatus.PermanentlyDenied)
        rule.onNodeWithText("Allow").assertDoesNotExist()
        rule.onNodeWithText("This permission was blocked.", substring = true).assertExists()
        rule.onNodeWithText("Open settings").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, settingsOpens)
        assertEquals(0, requests)
    }

    @Test
    fun grantedRendersNothing() {
        setCard(PermissionStatus.Granted)
        rule.onNodeWithText("Location").assertDoesNotExist()
        rule.onNodeWithText("Allow").assertDoesNotExist()
        rule.onNodeWithText("Open settings").assertDoesNotExist()
    }
}

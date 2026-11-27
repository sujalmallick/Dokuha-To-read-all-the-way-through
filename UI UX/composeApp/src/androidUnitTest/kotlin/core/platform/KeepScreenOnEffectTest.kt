package core.platform

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeepScreenOnEffectTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun keepScreenOnEffectTogglesAndRestoresViewKeepScreenOn() {
        var enabled by mutableStateOf(true)
        var showEffect by mutableStateOf(true)
        var capturedView: View? = null

        composeRule.setContent {
            capturedView = LocalView.current
            if (showEffect) {
                KeepScreenOnEffect(enabled = enabled)
            }
        }

        composeRule.waitForIdle()
        assertTrue(capturedView?.keepScreenOn == true)

        enabled = false
        composeRule.waitForIdle()
        assertFalse(capturedView?.keepScreenOn == true)

        enabled = true
        composeRule.waitForIdle()
        assertTrue(capturedView?.keepScreenOn == true)

        showEffect = false
        composeRule.waitForIdle()
        assertFalse(capturedView?.keepScreenOn == true)
    }
}

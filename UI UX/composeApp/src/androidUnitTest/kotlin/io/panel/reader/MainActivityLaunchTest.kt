package io.panel.reader

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MainActivityLaunchTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testLaunchMainActivity() {
        composeTestRule.waitForIdle()
        // Advance clock by 2500ms to trigger splash completion
        composeTestRule.mainClock.advanceTimeBy(2500L)
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().printToLog("MainActivityLaunchTest")
    }
}

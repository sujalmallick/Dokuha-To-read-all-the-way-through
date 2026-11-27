package ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ui.theme.PanelTheme
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class LiquidGlassIosMatrixSafetyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun glassSources_doNotCallUnsupportedIosMatrixOrScreenApisInCommonOrIosMain() {
        val projectRoot = generateSequence(File(System.getProperty("user.dir") ?: ".")) { it.parentFile }
            .firstOrNull { File(it, "composeApp/src/commonMain/kotlin/ui/glass").exists() }
            ?: error("Could not locate composeApp/src/commonMain/kotlin/ui/glass")

        val commonGlassFiles = File(projectRoot, "composeApp/src/commonMain/kotlin/ui/glass")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        val iosGlassFiles = File(projectRoot, "composeApp/src/iosMain/kotlin/ui/glass")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

        assertTrue(commonGlassFiles.isNotEmpty())
        assertTrue(iosGlassFiles.isNotEmpty())

        val forbiddenCalls = listOf(
            ".transformToScreen(",
            ".localToScreen(",
            "positionOnScreen(",
            ".getMatrix("
        )

        (commonGlassFiles + iosGlassFiles).forEach { file ->
            val content = file.readText()
            forbiddenCalls.forEach { forbidden ->
                assertFalse(
                    content.contains(forbidden),
                    "Forbidden iOS-unsupported API '$forbidden' found in ${file.name}"
                )
            }
        }
    }

    @Test
    fun glassSourceAndSurface_renderAndPositionWithoutMatrixException() {
        composeTestRule.setContent {
            PanelTheme {
                val glassState = rememberPanelGlassState()
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize().glassSource(glassState))
                    Box(modifier = Modifier.size(64.dp).glassSurface(state = glassState))
                }
            }
        }
        composeTestRule.waitForIdle()
    }
}

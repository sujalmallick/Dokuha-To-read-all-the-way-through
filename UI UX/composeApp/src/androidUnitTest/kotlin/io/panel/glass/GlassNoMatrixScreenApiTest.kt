package io.panel.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.glass.GlassVariant
import ui.glass.glassSource
import ui.glass.glassSurface
import ui.glass.rememberPanelGlassState
import ui.theme.PanelTheme
import java.io.File
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GlassNoMatrixScreenApiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun glassSourceFiles_doNotUseMatrixScreenCoordinateApis() {
        val forbiddenPatterns = listOf(
            Regex("""\btransformToScreen\b"""),
            Regex("""\blocalToScreen\b"""),
            Regex("""\btransformFrom\b"""),
            Regex("""\btransformToWindow\b"""),
            Regex("""(?<![A-Za-z0-9_])Matrix\(""")
        )
        val projectDir = File(System.getProperty("user.dir") ?: ".")
        val glassRoots = listOf(
            File(projectDir, "src/commonMain/kotlin/ui/glass"),
            File(projectDir, "src/iosMain/kotlin/ui/glass")
        ).filter { it.exists() }

        assertTrue(glassRoots.isNotEmpty(), "Expected ui/glass source directories under ${projectDir.absolutePath}")

        val violations = mutableListOf<String>()
        glassRoots.forEach { root ->
            root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    val codeOnly = file.readText()
                        .replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
                        .replace(Regex("//.*"), "")
                    forbiddenPatterns.forEach { pattern ->
                        if (pattern.containsMatchIn(codeOnly)) {
                            violations.add("${file.path}: matches forbidden matrix API '${pattern.pattern}'")
                        }
                    }
                }
        }

        assertTrue(
            violations.isEmpty(),
            "Matrix-based screen-coordinate APIs found in ui/glass:\n${violations.joinToString("\n")}"
        )
    }

    @Test
    fun glassSurface_rendersInsideScaledAndRotatedContainerWithoutMatrixScreenApi() {
        composeTestRule.setContent {
            PanelTheme {
                val glassState = rememberPanelGlassState()
                Box(modifier = Modifier.size(300.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .glassSource(glassState)
                            .background(Color(0xFF1E293B))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(width = 180.dp, height = 64.dp)
                            .graphicsLayer {
                                scaleX = 1.1f
                                scaleY = 1.1f
                                rotationZ = 5f
                            }
                            .glassSurface(
                                state = glassState,
                                variant = GlassVariant.Regular,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Glass OK", color = Color.White)
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Glass OK").assertExists()
    }
}

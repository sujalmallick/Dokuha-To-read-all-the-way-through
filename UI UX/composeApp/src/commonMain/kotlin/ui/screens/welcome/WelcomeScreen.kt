package ui.screens.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.glass.GlassVariant
import ui.glass.glassDimming
import ui.glass.glassPress
import ui.glass.glassSource
import ui.glass.glassSurface
import ui.glass.rememberPanelGlassState
import ui.screens.maintab.MainTabScreen
import ui.theme.CapsuleShape
import ui.theme.PanelBackground
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary

import data.settings.SettingsRepository
import org.koin.compose.koinInject

class WelcomeScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val settingsRepository: SettingsRepository = koinInject()
        WelcomeScreenContent(
            onGetStarted = {
                settingsRepository.setOnboardingCompleted(true)
                navigator.replaceAll(MainTabScreen())
            }
        )
    }
}

@Composable
fun WelcomeScreenContent(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        // LAYER 1: Background illustration & typography marked with glassSource
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassSource(glassState)
        ) {
            // Atmospheric Night Sky and Silhouette Illustration Canvas
            WelcomeIllustrationBackground(
                modifier = Modifier.fillMaxSize()
            )

            // Gradient overlay for contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            0.0f to Color(0x66080B11),
                            0.35f to Color.Transparent,
                            0.65f to Color(0xAA080B11),
                            1.0f to Color(0xF2080B11)
                        )
                    )
            )

            // Top branding & tagline area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(top = 80.dp)
                ) {
                    Text(
                        text = "Dokuha",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Read. Anywhere.",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Manga, Manhwa and Webnovels\nin one place.",
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary
                    )
                }
            }
        }

        // LAYER 2: Sibling Overlay - Get Started Liquid Glass CTA Capsule
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val buttonShape = RoundedCornerShape(12.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp)
                    .glassPress(interactionSource)
                    .glassDimming(alpha = 0.35f, shape = buttonShape)
                .glassSurface(
                    state = glassState,
                    variant = GlassVariant.Clear,
                    shape = buttonShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onGetStarted
                ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Clean, lightweight dark luxury gradient background for the welcome flow.
 */
@Composable
fun WelcomeIllustrationBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D111C),
                        Color(0xFF090D16),
                        Color(0xFF05070B)
                    )
                )
            )
    )
}

@Preview
@Composable
fun WelcomeScreenPreview() {
    PanelTheme {
        WelcomeScreenContent(onGetStarted = {})
    }
}

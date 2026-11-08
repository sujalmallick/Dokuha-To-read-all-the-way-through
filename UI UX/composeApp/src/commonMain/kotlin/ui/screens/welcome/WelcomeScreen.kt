package ui.screens.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.screens.library.LibraryScreen
import ui.theme.CapsuleShape
import ui.theme.ElectricBlue
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelBackground
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary

class WelcomeScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        WelcomeScreenContent(
            onGetStarted = {
                navigator.push(LibraryScreen())
            }
        )
    }
}

@Composable
fun WelcomeScreenContent(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
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

        // Main content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top branding & tagline area
            Column(
                modifier = Modifier.padding(top = 80.dp)
            ) {
                Text(
                    text = "Panel",
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

            // Bottom CTA Pill Button matching option 2.png
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(56.dp)
                        .clip(CapsuleShape)
                        .background(PanelSurfaceGlassHigh)
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.25f)
                                )
                            ),
                            shape = CapsuleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = Color.White),
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
}

/**
 * Procedural stylized background replicating the starry sky, meteor streak,
 * city twilight glow and rooftop silhouettes from option 2.png Screen 1.
 */
@Composable
fun WelcomeIllustrationBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Deep vertical twilight gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF080D1D), // Deep midnight blue
                    Color(0xFF141F3C), // Royal navy
                    Color(0xFF281C3F), // Purple dusk
                    Color(0xFF4C274B), // Magenta sunset horizon
                    Color(0xFF231628), // Lower dark shadow
                    Color(0xFF090C14)  // Base dark navy
                ),
                startY = 0f,
                endY = height
            )
        )

        // Meteor / Shooting star streak from top right toward center left
        val meteorStart = Offset(width * 0.85f, height * 0.10f)
        val meteorEnd = Offset(width * 0.40f, height * 0.28f)
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color(0xFF70B4FF), Color.White),
                start = meteorStart,
                end = meteorEnd
            ),
            start = meteorStart,
            end = meteorEnd,
            strokeWidth = 3f
        )
        // Meteor head glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFF60A5FA), Color.Transparent),
                center = meteorEnd,
                radius = 16f
            ),
            radius = 16f,
            center = meteorEnd
        )

        // Background twinkling stars
        val stars = listOf(
            Offset(width * 0.15f, height * 0.08f) to 2.5f,
            Offset(width * 0.28f, height * 0.16f) to 1.8f,
            Offset(width * 0.72f, height * 0.05f) to 2.2f,
            Offset(width * 0.88f, height * 0.22f) to 1.5f,
            Offset(width * 0.60f, height * 0.14f) to 2.0f,
            Offset(width * 0.08f, height * 0.26f) to 1.6f,
            Offset(width * 0.35f, height * 0.06f) to 2.4f,
            Offset(width * 0.52f, height * 0.24f) to 1.8f
        )
        for ((offset, r) in stars) {
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = r,
                center = offset
            )
        }

        // Horizon twilight glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF472B6).copy(alpha = 0.35f), Color.Transparent),
                center = Offset(width * 0.5f, height * 0.65f),
                radius = width * 0.7f
            ),
            center = Offset(width * 0.5f, height * 0.65f),
            radius = width * 0.7f
        )

        // City skyline silhouettes & Rooftop baseline
        val skylinePath = Path().apply {
            moveTo(0f, height * 0.72f)
            lineTo(width * 0.15f, height * 0.72f)
            lineTo(width * 0.15f, height * 0.67f)
            lineTo(width * 0.25f, height * 0.67f)
            lineTo(width * 0.25f, height * 0.70f)
            lineTo(width * 0.38f, height * 0.70f)
            lineTo(width * 0.38f, height * 0.65f)
            lineTo(width * 0.48f, height * 0.65f)
            lineTo(width * 0.48f, height * 0.73f)
            lineTo(width * 0.70f, height * 0.73f)
            lineTo(width * 0.70f, height * 0.68f)
            lineTo(width * 0.85f, height * 0.68f)
            lineTo(width * 0.85f, height * 0.74f)
            lineTo(width, height * 0.74f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(skylinePath, Color(0xFF131024).copy(alpha = 0.9f))

        // Foreground silhouette: Rooftop ledge where character sits
        val rooftopLedge = Path().apply {
            moveTo(0f, height * 0.82f)
            lineTo(width * 0.60f, height * 0.82f)
            lineTo(width * 0.65f, height * 0.78f)
            lineTo(width, height * 0.78f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(rooftopLedge, Color(0xFF090A12))

        // Character silhouette sitting on the ledge looking up
        val charCenter = Offset(width * 0.72f, height * 0.72f)
        drawCircle(
            color = Color(0xFF090A12),
            radius = 16f,
            center = Offset(charCenter.x, charCenter.y - 36f) // Head
        )
        val bodyPath = Path().apply {
            moveTo(charCenter.x - 18f, charCenter.y + 24f)
            lineTo(charCenter.x - 12f, charCenter.y - 20f)
            lineTo(charCenter.x + 14f, charCenter.y - 20f)
            lineTo(charCenter.x + 22f, charCenter.y + 24f)
            close()
        }
        drawPath(bodyPath, Color(0xFF090A12))
    }
}

@Preview
@Composable
fun WelcomeScreenPreview() {
    PanelTheme {
        WelcomeScreenContent(onGetStarted = {})
    }
}

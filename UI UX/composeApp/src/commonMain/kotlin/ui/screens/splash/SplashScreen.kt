package ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.settings.SettingsRepository
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import panelreader.composeapp.generated.resources.Res
import panelreader.composeapp.generated.resources.app_logo
import ui.screens.library.LibraryScreen
import ui.screens.welcome.WelcomeScreen
import ui.theme.ElectricBlue
import ui.theme.ElectricBlueLight
import ui.theme.PanelBackground
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.TextMuted
import ui.theme.TextPrimary
import ui.theme.TextSecondary

class SplashScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val settingsRepository: SettingsRepository = koinInject()

        SplashScreenContent(
            onFinished = {
                val isOnboarded = runCatching { settingsRepository.isOnboardingCompleted.value }.getOrDefault(false)
                if (isOnboarded) {
                    navigator.replaceAll(LibraryScreen())
                } else {
                    navigator.replaceAll(WelcomeScreen())
                }
            }
        )
    }
}

@Composable
fun SplashScreenContent(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.85f) }
    val progressAnim = remember { Animatable(0f) }
    var statusText by remember { mutableStateOf("Initializing Dokuha Engine...") }

    var hasFinished by remember { mutableStateOf(false) }
    val safeFinish = remember(onFinished) {
        {
            if (!hasFinished) {
                hasFinished = true
                onFinished()
            }
        }
    }

    // Breathing pulse for the halo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloRotation"
    )

    LaunchedEffect(Unit) {
        // Entrance animation
        alphaAnim.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(Unit) {
        progressAnim.animateTo(0.45f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        delay(400)
        statusText = "Loading Local Library & Sources..."
        progressAnim.animateTo(0.85f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        delay(350)
        statusText = "Ready"
        progressAnim.animateTo(1f, animationSpec = tween(250, easing = LinearEasing))
        delay(200)
        safeFinish()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = safeFinish
            )
    ) {
        // Background Ambient Aura Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val centerOffset = Offset(size.width * 0.5f, size.height * 0.42f)
            val glowRadius = (size.minDimension * 0.65f).coerceAtLeast(1f)

            // Radial electric-indigo ambient bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricBlue.copy(alpha = 0.28f),
                        Color(0xFF6366F1).copy(alpha = 0.15f),
                        Color(0xFF3B82F6).copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = glowRadius * pulseScale
                ),
                radius = glowRadius * pulseScale,
                center = centerOffset
            )
        }

        // Center Branded Content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo Container with Orbit Ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(140.dp)
                    .scale(scaleAnim.value * pulseScale)
                    .alpha(alphaAnim.value)
            ) {
                // Orbiting radiant ring
                Canvas(modifier = Modifier.size(136.dp)) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                ElectricBlueLight.copy(alpha = 0.6f),
                                ElectricBlue,
                                Color(0xFFA855F7).copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        ),
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )
                }

                // Inner glass bubble housing the official logo
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(PanelSurfaceGlassHigh)
                        .border(1.5.dp, Color.White.copy(alpha = 0.20f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.app_logo),
                        contentDescription = "Dokuha Logo",
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Brand Title: D O K U H A
            Text(
                text = "D O K U H A",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = 6.sp,
                modifier = Modifier.alpha(alphaAnim.value)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Kanji Mark: 「 読 破 」
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.alpha(alphaAnim.value)
            ) {
                Text(
                    text = "読",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricBlueLight,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "•",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "破",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricBlueLight,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle Tagline
            Text(
                text = "Immersive Manga, Manhwa & Novels",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.alpha(alphaAnim.value)
            )
        }

        // Bottom Loading & Engine Status
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 48.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant smooth liquid progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progressAnim.value)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(ElectricBlue, ElectricBlueLight, Color(0xFFA855F7))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    color = TextMuted,
                    letterSpacing = 0.3.sp
                )
                Text(
                    text = "v1.0.0",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

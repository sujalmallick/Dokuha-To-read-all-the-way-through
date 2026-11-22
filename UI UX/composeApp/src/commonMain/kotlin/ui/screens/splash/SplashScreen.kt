package ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.settings.AppLogoStyle
import data.settings.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import panelreader.composeapp.generated.resources.Res
import panelreader.composeapp.generated.resources.logo_classic
import panelreader.composeapp.generated.resources.logo_glassmorphic
import ui.screens.library.LibraryScreen
import ui.screens.welcome.WelcomeScreen

private val SplashBackground = Color(0xFF080808)
private val WordmarkColor = Color(0xFFEBE6E2)
private val KanjiColor = Color(0x66FFFFFF)

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

/**
 * Minimal, cinematic Dokuha Splash Screen with silky smooth logo entrance,
 * expanding typography, and organic background ribbon lighting.
 */
@Composable
fun SplashScreenContent(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    logoStyle: AppLogoStyle? = null
) {
    val settingsRepo: SettingsRepository? = runCatching { koinInject<SettingsRepository>() }.getOrNull()
    val currentSelectedStyle by (settingsRepo?.appLogoStyle ?: remember { MutableStateFlow(AppLogoStyle.GLASSMORPHIC) }).collectAsState()
    val activeStyle = logoStyle ?: currentSelectedStyle

    var hasFinished by remember { mutableStateOf(false) }
    val safeFinish = remember(onFinished) {
        {
            if (!hasFinished) {
                hasFinished = true
                onFinished()
            }
        }
    }

    // Logo entrance animatables
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.90f) }
    val glowIntensity = remember { Animatable(0f) }

    // Typography animatables
    val wordmarkAlpha = remember { Animatable(0f) }
    val wordmarkOffsetY = remember { Animatable(8f) }
    val wordmarkSpacing = remember { Animatable(6f) }
    val kanjiAlpha = remember { Animatable(0f) }

    // Breathing motion for background ribbon curves
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_breathe")
    val ribbonBreathe by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ribbon_breathe"
    )

    // Subtle gentle pulse for the logo once rested
    val logoPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_pulse"
    )

    // Choreograph the minimalist animation timeline
    LaunchedEffect(Unit) {
        // 1. Logo smooth entrance with soft scale settle
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, animationSpec = tween(850, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
        }
        launch {
            glowIntensity.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
        }

        // 2. Wordmark Reveal with expanding letter spacing
        launch {
            delay(550)
            launch {
                wordmarkAlpha.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
            }
            launch {
                wordmarkOffsetY.animateTo(0f, animationSpec = tween(650, easing = FastOutSlowInEasing))
            }
            launch {
                wordmarkSpacing.animateTo(11f, animationSpec = tween(750, easing = FastOutSlowInEasing))
            }
        }

        // 3. Kanji Mark soft reveal
        launch {
            delay(850)
            kanjiAlpha.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
        }

        // 4. Auto-transition into the app
        delay(2200)
        safeFinish()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = safeFinish
            )
    ) {
        // Background Ambient Canvas: Subtle radial glow + elegant flowing corner ribbons
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val center = Offset(w * 0.5f, h * 0.44f)

            // Dynamic ambient backlight behind logo
            val currentGlow = glowIntensity.value
            if (currentGlow > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF202025).copy(alpha = 0.85f * currentGlow),
                            Color(0xFF141417).copy(alpha = 0.50f * currentGlow),
                            SplashBackground
                        ),
                        center = center,
                        radius = (w * 0.72f).coerceAtLeast(1f)
                    ),
                    radius = (w * 0.72f).coerceAtLeast(1f),
                    center = center
                )
            }

            // Flowing translucent ribbon curves at Top-Left
            val tlShift = ribbonBreathe * 12f
            val pathTl1 = Path().apply {
                moveTo(0f, 0f)
                cubicTo(
                    w * 0.12f, h * 0.05f + tlShift,
                    w * 0.28f, h * 0.14f - tlShift,
                    0f, h * 0.32f
                )
                close()
            }
            drawPath(
                path = pathTl1,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2E2E32).copy(alpha = 0.28f),
                        Color(0xFF1C1C1F).copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w * 0.30f, h * 0.30f)
                )
            )

            val pathTl2 = Path().apply {
                moveTo(0f, h * 0.06f)
                cubicTo(
                    w * 0.20f, h * 0.10f - tlShift,
                    w * 0.38f, h * 0.20f + tlShift,
                    0f, h * 0.42f
                )
            }
            drawPath(
                path = pathTl2,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFCCCCCC).copy(alpha = 0.18f),
                        Color(0xFF888888).copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    start = Offset(0f, h * 0.06f),
                    end = Offset(w * 0.35f, h * 0.35f)
                ),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Flowing translucent ribbon curves at Bottom-Right
            val brShift = (1f - ribbonBreathe) * 10f
            val pathBr1 = Path().apply {
                moveTo(w, h * 0.62f)
                cubicTo(
                    w * 0.75f, h * 0.72f - brShift,
                    w * 0.52f, h * 0.86f + brShift,
                    w * 0.25f, h
                )
                lineTo(w, h)
                close()
            }
            drawPath(
                path = pathBr1,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF26262A).copy(alpha = 0.25f),
                        Color(0xFF18181B).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    start = Offset(w, h * 0.65f),
                    end = Offset(w * 0.35f, h)
                )
            )

            val pathBr2 = Path().apply {
                moveTo(w, h * 0.72f)
                cubicTo(
                    w * 0.82f, h * 0.80f + brShift,
                    w * 0.68f, h * 0.90f - brShift,
                    w * 0.45f, h
                )
            }
            drawPath(
                path = pathBr2,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFDDDDDD).copy(alpha = 0.22f),
                        Color(0xFF777777).copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    start = Offset(w, h * 0.72f),
                    end = Offset(w * 0.45f, h)
                ),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        // Center Branded Content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val logoSize = 188.dp
            val masterLogoRes = if (activeStyle == AppLogoStyle.GLASSMORPHIC) {
                Res.drawable.logo_glassmorphic
            } else {
                Res.drawable.logo_classic
            }

            // Pristine, untouched master logo with silky smooth entrance & subtle idle breathing
            Box(
                modifier = Modifier
                    .size(logoSize)
                    .graphicsLayer {
                        scaleX = logoScale.value * logoPulse
                        scaleY = logoScale.value * logoPulse
                        alpha = logoAlpha.value
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(masterLogoRes),
                    contentDescription = "Dokuha Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            // Thin, modern, widely spaced uppercase "D O K U H A"
            Text(
                text = "D O K U H A",
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = WordmarkColor,
                letterSpacing = wordmarkSpacing.value.sp,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = wordmarkAlpha.value
                        translationY = wordmarkOffsetY.value
                    }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Kanji Mark: 「 読 破 」
            Text(
                text = "「 読 破 」",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = KanjiColor,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(kanjiAlpha.value * 0.8f)
            )
        }

        // Minimalist Bottom Branding / Tap to skip hint
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .alpha(wordmarkAlpha.value * 0.4f)
        ) {
            Text(
                text = "Tap anywhere to skip",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )
        }
    }
}

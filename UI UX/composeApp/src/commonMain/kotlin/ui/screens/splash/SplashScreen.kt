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
import core.platform.PlatformInfo
import data.settings.AppLogoStyle
import data.settings.SettingsRepository
import data.settings.SplashMotionStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import panelreader.composeapp.generated.resources.Res
import panelreader.composeapp.generated.resources.logo_classic
import panelreader.composeapp.generated.resources.logo_glassmorphic
import ui.components.LiquidGlassEnding
import ui.components.LiquidGlassLogo
import ui.screens.maintab.MainTabScreen
import ui.screens.welcome.WelcomeScreen

private val SplashBackground = Color(0xFF080808)
private val WordmarkColor = Color(0xFFEBE6E2)
private val KanjiColor = Color(0x66FFFFFF)

class SplashScreen(
    private val motionStyle: SplashMotionStyle? = null,
    private val logoStyle: AppLogoStyle? = null
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val settingsRepository: SettingsRepository = koinInject()

        SplashScreenContent(
            logoStyle = logoStyle,
            motionStyle = motionStyle,
            onFinished = {
                val isOnboarded = runCatching { settingsRepository.isOnboardingCompleted.value }.getOrDefault(false)
                if (navigator.size > 1) {
                    navigator.pop()
                } else if (isOnboarded) {
                    navigator.replaceAll(MainTabScreen())
                } else {
                    navigator.replaceAll(WelcomeScreen())
                }
            }
        )
    }
}

/**
 * Multi-concept Dokuha Splash Screen with silky smooth logo entrance,
 * expanding typography, and 6 custom motion styles (including Lunar Silver Glow
 * with smooth dissipating ending and Liquid Glass for mobile).
 */
@Composable
fun SplashScreenContent(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    logoStyle: AppLogoStyle? = null,
    motionStyle: SplashMotionStyle? = null
) {
    val settingsRepo: SettingsRepository? = runCatching { koinInject<SettingsRepository>() }.getOrNull()
    val currentSelectedStyle by (settingsRepo?.appLogoStyle ?: remember { MutableStateFlow(AppLogoStyle.GLASSMORPHIC) }).collectAsState()
    val currentSelectedMotion by (settingsRepo?.splashMotionStyle ?: remember { MutableStateFlow(SplashMotionStyle.LUNAR_SILVER_GLOW) }).collectAsState()
    val activeStyle = logoStyle ?: currentSelectedStyle
    val activeMotion = motionStyle ?: currentSelectedMotion

    var hasFinished by remember { mutableStateOf(false) }
    val safeFinish = remember(onFinished) {
        {
            if (!hasFinished) {
                hasFinished = true
                onFinished()
            }
        }
    }

    if (activeMotion == SplashMotionStyle.LIQUID_GLASS) {
        // Concept 6: ReactDrops Liquid Glass Logo entrance
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SplashBackground)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = safeFinish
                ),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassLogo(
                logo = if (activeStyle == AppLogoStyle.GLASSMORPHIC) Res.drawable.logo_glassmorphic else Res.drawable.logo_classic,
                ending = LiquidGlassEnding.LOGO,
                onAnimationEnd = safeFinish
            )
        }
        return
    }

    // Logo entrance animatables
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.90f) }
    val glowIntensity = remember { Animatable(0f) }
    val glowScale = remember { Animatable(1.0f) }

    // Typography animatables
    val wordmarkAlpha = remember { Animatable(0f) }
    val wordmarkOffsetY = remember { Animatable(8f) }
    val wordmarkSpacing = remember { Animatable(6f) }
    val kanjiAlpha = remember { Animatable(0f) }

    // Specialized motion animatables
    val sliceProgress = remember { Animatable(0f) }
    val sliceShine = remember { Animatable(0f) }
    val inkRipple = remember { Animatable(0f) }
    val prismAngle = remember { Animatable(0f) }

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

    // Choreograph the animation timeline
    LaunchedEffect(activeMotion) {
        // 1. Logo smooth entrance with soft scale settle
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, animationSpec = tween(850, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
        }

        when (activeMotion) {
            SplashMotionStyle.LUNAR_SILVER_GLOW -> {
                // Concept 5: Lunar Silver Glow with smooth dissipating ending
                launch {
                    glowIntensity.animateTo(1f, animationSpec = tween(750, easing = FastOutSlowInEasing))
                    delay(600)
                    // Smooth, elegant fade-out & inward dissipation at ending
                    launch {
                        glowIntensity.animateTo(0f, animationSpec = tween(850, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)))
                    }
                    launch {
                        glowScale.animateTo(0.72f, animationSpec = tween(850, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)))
                    }
                }
            }
            SplashMotionStyle.MANGA_RAZOR_SLICE -> {
                // Concept 2: Katana slice across screen with silver sheen
                launch {
                    glowIntensity.animateTo(0.9f, animationSpec = tween(500, easing = FastOutSlowInEasing))
                }
                launch {
                    sliceProgress.animateTo(1f, animationSpec = tween(700, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
                }
                launch {
                    sliceShine.animateTo(1f, animationSpec = tween(450, easing = LinearEasing))
                    sliceShine.animateTo(0f, animationSpec = tween(350, easing = LinearEasing))
                }
            }
            SplashMotionStyle.INK_DROP -> {
                // Concept 3: Sumi-e ink drop expansion
                launch {
                    inkRipple.animateTo(1f, animationSpec = tween(1300, easing = FastOutSlowInEasing))
                }
                launch {
                    glowIntensity.animateTo(0.65f, animationSpec = tween(800, easing = FastOutSlowInEasing))
                }
            }
            SplashMotionStyle.DIMENSIONAL_GLASS -> {
                // Concept 4: Prism flare and glass warp
                launch {
                    glowIntensity.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
                }
                launch {
                    prismAngle.animateTo(360f, animationSpec = tween(2200, easing = LinearEasing))
                }
            }
            SplashMotionStyle.AMBIENT_RIBBON -> {
                // Concept 1: Ambient Ribbon
                launch {
                    glowIntensity.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
                }
            }
            SplashMotionStyle.LIQUID_GLASS -> {}
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
        delay(2250)
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
        // Canvas background rendering based on active concept
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val center = Offset(w * 0.5f, h * 0.44f)

            when (activeMotion) {
                SplashMotionStyle.LUNAR_SILVER_GLOW -> {
                    // Concept 5: Lunar Silver Glow with smooth dissipating ending
                    val currentGlow = glowIntensity.value
                    val currentScale = glowScale.value
                    if (currentGlow > 0f) {
                        val baseRadius = (w * 0.72f * currentScale).coerceAtLeast(1f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFF8FAFC).copy(alpha = 0.85f * currentGlow),
                                    Color(0xFFE2E8F0).copy(alpha = 0.55f * currentGlow),
                                    Color(0xFF94A3B8).copy(alpha = 0.28f * currentGlow),
                                    Color(0xFF334155).copy(alpha = 0.08f * currentGlow),
                                    SplashBackground
                                ),
                                center = center,
                                radius = baseRadius
                            ),
                            radius = baseRadius,
                            center = center
                        )

                        // Pearlescent lunar ring
                        drawCircle(
                            color = Color(0xFFE2E8F0).copy(alpha = 0.32f * currentGlow),
                            radius = (w * 0.30f * currentScale).coerceAtLeast(1f),
                            center = center,
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }
                }
                SplashMotionStyle.MANGA_RAZOR_SLICE -> {
                    // Concept 2: Manga Razor Slice with silver edge gleam
                    val progress = sliceProgress.value
                    val shine = sliceShine.value

                    // Background ambient glow behind logo
                    if (glowIntensity.value > 0f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF262A30).copy(alpha = 0.75f * glowIntensity.value),
                                    Color(0xFF14161A).copy(alpha = 0.40f * glowIntensity.value),
                                    SplashBackground
                                ),
                                center = center,
                                radius = (w * 0.65f).coerceAtLeast(1f)
                            ),
                            radius = (w * 0.65f).coerceAtLeast(1f),
                            center = center
                        )
                    }

                    // Diagonal katana cut line from top-right to bottom-left
                    val startX = w * 1.1f
                    val startY = h * 0.15f
                    val endX = -w * 0.1f
                    val endY = h * 0.75f

                    val currentCutX = startX + (endX - startX) * progress
                    val currentCutY = startY + (endY - startY) * progress

                    // Blade cut shine line
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFE2E8F0).copy(alpha = 0.90f * shine),
                                Color.White.copy(alpha = shine),
                                Color(0xFFCBD5E1).copy(alpha = 0.80f * shine),
                                Color.Transparent
                            ),
                            start = Offset(startX, startY),
                            end = Offset(currentCutX, currentCutY)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(currentCutX, currentCutY),
                        strokeWidth = 2.5.dp.toPx()
                    )
                }
                SplashMotionStyle.INK_DROP -> {
                    // Concept 3: Sumi-e Ink Drop Expansion
                    val ripple = inkRipple.value
                    if (ripple > 0f) {
                        // Expanding concentric ink wash ripples
                        val r1 = (w * 0.35f * ripple).coerceAtLeast(1f)
                        val r2 = (w * 0.55f * ripple).coerceAtLeast(1f)
                        val r3 = (w * 0.75f * ripple).coerceAtLeast(1f)

                        drawCircle(
                            color = Color(0xFF26262B).copy(alpha = (1f - ripple) * 0.35f),
                            radius = r1,
                            center = center
                        )
                        drawCircle(
                            color = Color(0xFF1E1E22).copy(alpha = (1f - ripple) * 0.25f),
                            radius = r2,
                            center = center
                        )
                        drawCircle(
                            color = Color(0xFF18181B).copy(alpha = (1f - ripple) * 0.18f),
                            radius = r3,
                            center = center
                        )
                    }
                }
                SplashMotionStyle.DIMENSIONAL_GLASS -> {
                    // Concept 4: Prism flare and glass refraction
                    val currentGlow = glowIntensity.value
                    if (currentGlow > 0f) {
                        // Horizontal anamorphic prism flare
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8).copy(alpha = 0.30f * currentGlow),
                                    Color(0xFFA855F7).copy(alpha = 0.22f * currentGlow),
                                    Color(0xFF0F172A).copy(alpha = 0.10f * currentGlow),
                                    SplashBackground
                                ),
                                center = center,
                                radius = (w * 0.70f).coerceAtLeast(1f)
                            ),
                            radius = (w * 0.70f).coerceAtLeast(1f),
                            center = center
                        )

                        // Prismatic lens ring
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8).copy(alpha = 0.35f * currentGlow),
                                    Color(0xFFA855F7).copy(alpha = 0.35f * currentGlow),
                                    Color(0xFFF43F5E).copy(alpha = 0.25f * currentGlow),
                                    Color(0xFF38BDF8).copy(alpha = 0.35f * currentGlow)
                                ),
                                center = center
                            ),
                            radius = (w * 0.32f).coerceAtLeast(1f),
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
                SplashMotionStyle.AMBIENT_RIBBON, SplashMotionStyle.LIQUID_GLASS -> {
                    // Concept 1: Flowing ribbons + ambient backlight
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

                    // Top-Left ribbon curve
                    val tlShift = ribbonBreathe * 12f
                    val pathTl1 = Path().apply {
                        moveTo(0f, 0f)
                        cubicTo(w * 0.12f, h * 0.05f + tlShift, w * 0.28f, h * 0.14f - tlShift, 0f, h * 0.32f)
                        close()
                    }
                    drawPath(
                        path = pathTl1,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF2E2E32).copy(alpha = 0.28f), Color(0xFF1C1C1F).copy(alpha = 0.10f), Color.Transparent),
                            start = Offset(0f, 0f),
                            end = Offset(w * 0.30f, h * 0.30f)
                        )
                    )

                    // Bottom-Right ribbon curve
                    val brShift = (1f - ribbonBreathe) * 10f
                    val pathBr1 = Path().apply {
                        moveTo(w, h * 0.62f)
                        cubicTo(w * 0.75f, h * 0.72f - brShift, w * 0.52f, h * 0.86f + brShift, w * 0.25f, h)
                        lineTo(w, h)
                        close()
                    }
                    drawPath(
                        path = pathBr1,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF26262A).copy(alpha = 0.25f), Color(0xFF18181B).copy(alpha = 0.08f), Color.Transparent),
                            start = Offset(w, h * 0.65f),
                            end = Offset(w * 0.35f, h)
                        )
                    )
                }
            }
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

            // Uppercase "D O K U H A"
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

        // Tap to skip hint
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

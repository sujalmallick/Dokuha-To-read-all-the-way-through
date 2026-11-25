package ui.screens.splash

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
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
import ui.theme.GlassBorder
import ui.theme.LocalReduceMotion

private val SplashBackground = Color(0xFF080808)
private val WordmarkColor = Color(0xFFEBE6E2)
private val KanjiColor = Color(0x80FFFFFF)

internal data class SplashFormatEntry(
    val label: String,
    val accentColor: Color
)

internal val SplashFormatEntries = listOf(
    SplashFormatEntry("MANHWA", Color(0xFF38BDF8)),
    SplashFormatEntry("MANHUA", Color(0xFFF59E0B)),
    SplashFormatEntry("MANGA", Color(0xFFE2E8F0)),
    SplashFormatEntry("COMIC", Color(0xFFF43F5E)),
    SplashFormatEntry("WEBNOVEL", Color(0xFFA855F7))
)

class SplashScreen(
    private val motionStyle: SplashMotionStyle? = null,
    private val logoStyle: AppLogoStyle? = null,
    private val isPreview: Boolean = false
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val settingsRepository: SettingsRepository = koinInject()
        var replayKey by remember { mutableIntStateOf(0) }

        Box(modifier = Modifier.fillMaxSize()) {
            key(replayKey) {
                SplashScreenContent(
                    logoStyle = logoStyle,
                    motionStyle = motionStyle,
                    autoFinish = !isPreview,
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

            if (isPreview) {
                // Floating top controls: Close and Replay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.dp, GlassBorder, CircleShape)
                            .clickable { navigator.pop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Preview",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.dp, GlassBorder, CircleShape)
                            .clickable { replayKey++ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Replay Preview",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Multi-concept Dokuha Splash Screen with silky smooth logo entrance,
 * expanding typography, rotating format tagline (MANHWA / MANHUA / MANGA / COMIC / WEBNOVEL),
 * and 6 custom motion styles that adapt cleanly to both fullscreen and compact preview cards.
 */
@Composable
fun SplashScreenContent(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    logoStyle: AppLogoStyle? = null,
    motionStyle: SplashMotionStyle? = null,
    autoFinish: Boolean = true
) {
    val settingsRepo: SettingsRepository? = runCatching { koinInject<SettingsRepository>() }.getOrNull()
    val currentSelectedStyle by (settingsRepo?.appLogoStyle ?: remember { MutableStateFlow(AppLogoStyle.GLASSMORPHIC) }).collectAsState()
    val currentSelectedMotion by (settingsRepo?.splashMotionStyle ?: remember { MutableStateFlow(SplashMotionStyle.LUNAR_SILVER_GLOW) }).collectAsState()
    val activeStyle = logoStyle ?: currentSelectedStyle
    val activeMotion = motionStyle ?: currentSelectedMotion
    val reduceMotion = LocalReduceMotion.current

    var hasFinished by remember { mutableStateOf(false) }
    val safeFinish: () -> Unit = remember(onFinished) {
        {
            if (!hasFinished) {
                hasFinished = true
                onFinished()
            }
        }
    }

    // Logo entrance animatables
    val logoAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val logoScale = remember { Animatable(if (reduceMotion) 1f else 0.90f) }
    val glowIntensity = remember { Animatable(if (reduceMotion) 0.35f else 0f) }
    val glowScale = remember { Animatable(1.0f) }

    // Typography animatables
    val wordmarkAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val wordmarkOffsetY = remember { Animatable(if (reduceMotion) 0f else 8f) }
    val wordmarkSpacing = remember { Animatable(if (reduceMotion) 11f else 6f) }
    val kanjiAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val formatAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    var formatIndex by remember { mutableIntStateOf(0) }

    // Specialized motion animatables
    val sliceProgress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val sliceShine = remember { Animatable(if (reduceMotion) 0.2f else 0f) }
    val sliceFlash = remember { Animatable(0f) }
    val inkRipple = remember { Animatable(if (reduceMotion) 0.7f else 0f) }
    val prismAngle = remember { Animatable(0f) }

    // Breathing motion for background ribbon curves
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_breathe")
    val ribbonBreathe by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ribbon_breathe"
    )

    val logoPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (reduceMotion) 1f else 1.022f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_pulse"
    )

    // Rotate MANHWA -> MANHUA -> MANGA -> COMIC -> WEBNOVEL seamlessly on the single screen
    LaunchedEffect(activeMotion, autoFinish, reduceMotion) {
        formatIndex = 0
        if (reduceMotion) return@LaunchedEffect
        delay(650)
        val stepMs = if (autoFinish) 420L else 850L
        while (true) {
            delay(stepMs)
            formatIndex = (formatIndex + 1) % SplashFormatEntries.size
        }
    }

    // Choreograph the active concept timeline
    LaunchedEffect(activeMotion, reduceMotion) {
        if (reduceMotion) {
            logoAlpha.snapTo(1f)
            logoScale.snapTo(1f)
            glowIntensity.snapTo(0.35f)
            wordmarkAlpha.snapTo(1f)
            wordmarkOffsetY.snapTo(0f)
            wordmarkSpacing.snapTo(11f)
            kanjiAlpha.snapTo(1f)
            formatAlpha.snapTo(1f)
            if (autoFinish) {
                delay(900)
                safeFinish()
            }
            return@LaunchedEffect
        }

        // 1. Logo entrance with smooth scale settle
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, animationSpec = tween(820, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
        }

        when (activeMotion) {
            SplashMotionStyle.LUNAR_SILVER_GLOW -> {
                // Concept 5: Lunar Silver Glow with smooth soft dissipation into resting aura
                launch {
                    glowIntensity.animateTo(1f, animationSpec = tween(720, easing = FastOutSlowInEasing))
                    delay(520)
                    launch {
                        glowIntensity.animateTo(
                            0.24f,
                            animationSpec = tween(900, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f))
                        )
                    }
                    launch {
                        glowScale.animateTo(
                            0.84f,
                            animationSpec = tween(900, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f))
                        )
                    }
                }
            }
            SplashMotionStyle.MANGA_RAZOR_SLICE -> {
                // Concept 2: Katana slice across screen with center flash and silver edge gleam
                launch {
                    glowIntensity.animateTo(0.95f, animationSpec = tween(550, easing = FastOutSlowInEasing))
                }
                launch {
                    sliceProgress.animateTo(1f, animationSpec = tween(680, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
                }
                launch {
                    sliceShine.animateTo(1f, animationSpec = tween(420, easing = LinearEasing))
                    launch {
                        sliceFlash.animateTo(1f, animationSpec = tween(160, easing = FastOutSlowInEasing))
                        sliceFlash.animateTo(0f, animationSpec = tween(480, easing = FastOutSlowInEasing))
                    }
                    sliceShine.animateTo(0.22f, animationSpec = tween(520, easing = FastOutSlowInEasing))
                }
            }
            SplashMotionStyle.INK_DROP -> {
                // Concept 3: Sumi-e ink drop expansion with visible ring aura
                launch {
                    inkRipple.animateTo(1f, animationSpec = tween(1500, easing = FastOutSlowInEasing))
                }
                launch {
                    glowIntensity.animateTo(0.85f, animationSpec = tween(750, easing = FastOutSlowInEasing))
                    delay(500)
                    glowIntensity.animateTo(0.38f, animationSpec = tween(900, easing = FastOutSlowInEasing))
                }
            }
            SplashMotionStyle.DIMENSIONAL_GLASS -> {
                // Concept 4: Orbiting prismatic lens flare and spectral halo
                launch {
                    glowIntensity.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
                }
                launch {
                    prismAngle.animateTo(360f, animationSpec = tween(2500, easing = LinearEasing))
                }
            }
            SplashMotionStyle.AMBIENT_RIBBON -> {
                // Concept 1: Breathing architectural ribbons & soft backlight
                launch {
                    glowIntensity.animateTo(1f, animationSpec = tween(850, easing = FastOutSlowInEasing))
                }
            }
            SplashMotionStyle.LIQUID_GLASS -> {
                // Concept 6: Ambient backlight beneath 3D LiquidGlassLogo
                launch {
                    glowIntensity.animateTo(0.85f, animationSpec = tween(850, easing = FastOutSlowInEasing))
                }
            }
        }

        // 2. Wordmark reveal with expanding letter spacing
        launch {
            delay(450)
            launch {
                wordmarkAlpha.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
            }
            launch {
                wordmarkOffsetY.animateTo(0f, animationSpec = tween(600, easing = FastOutSlowInEasing))
            }
            launch {
                wordmarkSpacing.animateTo(11f, animationSpec = tween(720, easing = FastOutSlowInEasing))
            }
        }

        // 3. Kanji + rotating format label reveal
        launch {
            delay(700)
            kanjiAlpha.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
        }
        launch {
            delay(780)
            formatAlpha.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
        }

        // 4. Auto-transition after cycling through formats
        if (autoFinish) {
            delay(2750)
            safeFinish()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = safeFinish
            )
    ) {
        val isCompact = maxHeight < 360.dp
        val logoSize = if (isCompact) {
            (maxHeight * 0.36f).coerceIn(52.dp, 82.dp)
        } else {
            188.dp
        }
        val wordmarkGap = if (isCompact) 8.dp else 28.dp
        val kanjiGap = if (isCompact) 3.dp else 9.dp
        val formatGap = if (isCompact) 7.dp else 14.dp
        val wordmarkFontSize = if (isCompact) 13.sp else 20.sp
        val kanjiFontSize = if (isCompact) 9.sp else 11.sp
        val letterSpacingSp = if (isCompact) {
            (wordmarkSpacing.value * 0.65f).sp
        } else {
            wordmarkSpacing.value.sp
        }

        // Canvas background rendering based on active concept
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val center = Offset(w * 0.5f, h * if (isCompact) 0.36f else 0.43f)
            val radiusBase = minOf(w, h)

            when (activeMotion) {
                SplashMotionStyle.LUNAR_SILVER_GLOW -> {
                    // Concept 5: Lunar Silver Glow
                    val currentGlow = glowIntensity.value
                    val currentScale = glowScale.value
                    if (currentGlow > 0f) {
                        val baseRadius = (radiusBase * 0.68f * currentScale).coerceAtLeast(1f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFF8FAFC).copy(alpha = 0.72f * currentGlow),
                                    Color(0xFFE2E8F0).copy(alpha = 0.44f * currentGlow),
                                    Color(0xFF94A3B8).copy(alpha = 0.20f * currentGlow),
                                    Color(0xFF334155).copy(alpha = 0.06f * currentGlow),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = baseRadius
                            ),
                            radius = baseRadius,
                            center = center
                        )

                        drawCircle(
                            color = Color(0xFFE2E8F0).copy(alpha = 0.36f * currentGlow),
                            radius = (radiusBase * 0.30f * currentScale).coerceAtLeast(1f),
                            center = center,
                            style = Stroke(width = 1.25.dp.toPx())
                        )
                    }
                }
                SplashMotionStyle.MANGA_RAZOR_SLICE -> {
                    // Concept 2: Manga Razor Slice with impact flash & silver gleam
                    val progress = sliceProgress.value
                    val shine = sliceShine.value
                    val flash = sliceFlash.value

                    if (glowIntensity.value > 0f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF334155).copy(alpha = 0.55f * glowIntensity.value),
                                    Color(0xFF1E293B).copy(alpha = 0.25f * glowIntensity.value),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = (radiusBase * 0.62f).coerceAtLeast(1f)
                            ),
                            radius = (radiusBase * 0.62f).coerceAtLeast(1f),
                            center = center
                        )
                    }

                    if (flash > 0f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.55f * flash),
                                    Color(0xFF93C5FD).copy(alpha = 0.25f * flash),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = (radiusBase * 0.45f * (0.6f + 0.4f * flash)).coerceAtLeast(1f)
                            ),
                            radius = (radiusBase * 0.45f).coerceAtLeast(1f),
                            center = center
                        )
                    }

                    val startX = w * 1.12f
                    val startY = h * 0.12f
                    val endX = -w * 0.12f
                    val endY = h * 0.78f
                    val currentCutX = startX + (endX - startX) * progress
                    val currentCutY = startY + (endY - startY) * progress

                    if (progress > 0.01f && shine > 0.01f) {
                        // Soft outer steel aura
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF94A3B8).copy(alpha = 0.45f * shine),
                                    Color(0xFFE2E8F0).copy(alpha = 0.55f * shine),
                                    Color.Transparent
                                ),
                                start = Offset(startX, startY),
                                end = Offset(currentCutX, currentCutY)
                            ),
                            start = Offset(startX, startY),
                            end = Offset(currentCutX, currentCutY),
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        // Sharp silver katana edge
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFE2E8F0).copy(alpha = 0.92f * shine),
                                    Color.White.copy(alpha = shine),
                                    Color(0xFFCBD5E1).copy(alpha = 0.80f * shine),
                                    Color.Transparent
                                ),
                                start = Offset(startX, startY),
                                end = Offset(currentCutX, currentCutY)
                            ),
                            start = Offset(startX, startY),
                            end = Offset(currentCutX, currentCutY),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                SplashMotionStyle.INK_DROP -> {
                    // Concept 3: Sumi-e Ink Drop Expansion + core bloom
                    val glow = glowIntensity.value
                    if (glow > 0f) {
                        val inkCoreRadius = (radiusBase * 0.58f).coerceAtLeast(1f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF3F3F46).copy(alpha = 0.58f * glow),
                                    Color(0xFF27272A).copy(alpha = 0.32f * glow),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = inkCoreRadius
                            ),
                            radius = inkCoreRadius,
                            center = center
                        )
                    }

                    val ripple = inkRipple.value
                    if (ripple > 0f) {
                        val ringAlpha = (1f - ripple * 0.72f).coerceIn(0f, 1f)
                        val r1 = (radiusBase * 0.24f * (0.4f + ripple)).coerceAtLeast(1f)
                        val r2 = (radiusBase * 0.42f * (0.3f + ripple)).coerceAtLeast(1f)
                        val r3 = (radiusBase * 0.62f * (0.2f + ripple)).coerceAtLeast(1f)

                        drawCircle(
                            color = Color(0xFF3F3F46).copy(alpha = 0.30f * ringAlpha),
                            radius = r1,
                            center = center
                        )
                        drawCircle(
                            color = Color(0xFFA1A1AA).copy(alpha = 0.28f * ringAlpha),
                            radius = r1,
                            center = center,
                            style = Stroke(width = 1.4.dp.toPx())
                        )
                        drawCircle(
                            color = Color(0xFF27272A).copy(alpha = 0.24f * ringAlpha),
                            radius = r2,
                            center = center
                        )
                        drawCircle(
                            color = Color(0xFF71717A).copy(alpha = 0.22f * ringAlpha),
                            radius = r2,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawCircle(
                            color = Color(0xFF52525B).copy(alpha = 0.16f * ringAlpha),
                            radius = r3,
                            center = center,
                            style = Stroke(width = 0.8.dp.toPx())
                        )
                    }
                }
                SplashMotionStyle.DIMENSIONAL_GLASS -> {
                    // Concept 4: Rotating Prism Flare and Chromatic Dispersion Halo
                    val currentGlow = glowIntensity.value
                    val angle = prismAngle.value
                    if (currentGlow > 0f) {
                        val flareRadius = (radiusBase * 0.68f).coerceAtLeast(1f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8).copy(alpha = 0.32f * currentGlow),
                                    Color(0xFFA855F7).copy(alpha = 0.24f * currentGlow),
                                    Color(0xFF0F172A).copy(alpha = 0.12f * currentGlow),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = flareRadius
                            ),
                            radius = flareRadius,
                            center = center
                        )

                        // Horizontal anamorphic optical streak
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF38BDF8).copy(alpha = 0.35f * currentGlow),
                                    Color.White.copy(alpha = 0.45f * currentGlow),
                                    Color(0xFFA855F7).copy(alpha = 0.35f * currentGlow),
                                    Color.Transparent
                                ),
                                startX = w * 0.08f,
                                endX = w * 0.92f
                            ),
                            start = Offset(w * 0.08f, center.y),
                            end = Offset(w * 0.92f, center.y),
                            strokeWidth = 1.5.dp.toPx()
                        )

                        rotate(degrees = angle, pivot = center) {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF38BDF8).copy(alpha = 0.55f * currentGlow),
                                        Color(0xFFA855F7).copy(alpha = 0.45f * currentGlow),
                                        Color(0xFFF43F5E).copy(alpha = 0.38f * currentGlow),
                                        Color(0xFF22D3EE).copy(alpha = 0.45f * currentGlow),
                                        Color(0xFF38BDF8).copy(alpha = 0.55f * currentGlow)
                                    ),
                                    center = center
                                ),
                                radius = (radiusBase * 0.32f).coerceAtLeast(1f),
                                center = center,
                                style = Stroke(width = 1.8.dp.toPx())
                            )
                        }

                        rotate(degrees = -angle * 0.6f, pivot = center) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFFA855F7).copy(alpha = 0.40f * currentGlow),
                                        Color.White.copy(alpha = 0.45f * currentGlow),
                                        Color.Transparent
                                    ),
                                    center = center
                                ),
                                startAngle = 0f,
                                sweepAngle = 140f,
                                useCenter = false,
                                topLeft = Offset(center.x - radiusBase * 0.40f, center.y - radiusBase * 0.40f),
                                size = Size(radiusBase * 0.80f, radiusBase * 0.80f),
                                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
                SplashMotionStyle.AMBIENT_RIBBON, SplashMotionStyle.LIQUID_GLASS -> {
                    // Concept 1 & Concept 6 backdrop: Flowing volumetric ribbons + backlight
                    val currentGlow = glowIntensity.value
                    if (currentGlow > 0f) {
                        val glowTint = if (activeMotion == SplashMotionStyle.LIQUID_GLASS) {
                            Color(0xFF1D4ED8).copy(alpha = 0.24f * currentGlow)
                        } else {
                            Color(0xFF334155).copy(alpha = 0.45f * currentGlow)
                        }
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowTint,
                                    Color(0xFF18181B).copy(alpha = 0.45f * currentGlow),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = (radiusBase * 0.72f).coerceAtLeast(1f)
                            ),
                            radius = (radiusBase * 0.72f).coerceAtLeast(1f),
                            center = center
                        )
                    }

                    val tlShift = ribbonBreathe * if (isCompact) 14f else 36f
                    val pathTl1 = Path().apply {
                        moveTo(0f, 0f)
                        cubicTo(
                            w * 0.14f, h * 0.06f + tlShift,
                            w * 0.34f, h * 0.16f - tlShift,
                            0f, h * 0.38f
                        )
                        close()
                    }
                    drawPath(
                        path = pathTl1,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3F3F46).copy(alpha = 0.42f),
                                Color(0xFF27272A).copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w * 0.34f, h * 0.38f)
                        )
                    )
                    drawPath(
                        path = Path().apply {
                            moveTo(0f, h * 0.38f)
                            cubicTo(
                                w * 0.34f, h * 0.16f - tlShift,
                                w * 0.14f, h * 0.06f + tlShift,
                                0f, 0f
                            )
                        },
                        color = Color.White.copy(alpha = 0.10f),
                        style = Stroke(width = 0.9.dp.toPx())
                    )

                    val brShift = (1f - ribbonBreathe) * if (isCompact) 12f else 32f
                    val pathBr1 = Path().apply {
                        moveTo(w, h * 0.56f)
                        cubicTo(
                            w * 0.72f, h * 0.70f - brShift,
                            w * 0.50f, h * 0.85f + brShift,
                            w * 0.18f, h
                        )
                        lineTo(w, h)
                        close()
                    }
                    drawPath(
                        path = pathBr1,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3F3F46).copy(alpha = 0.38f),
                                Color(0xFF27272A).copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            start = Offset(w, h * 0.58f),
                            end = Offset(w * 0.24f, h)
                        )
                    )
                    drawPath(
                        path = Path().apply {
                            moveTo(w, h * 0.56f)
                            cubicTo(
                                w * 0.72f, h * 0.70f - brShift,
                                w * 0.50f, h * 0.85f + brShift,
                                w * 0.18f, h
                            )
                        },
                        color = Color.White.copy(alpha = 0.09f),
                        style = Stroke(width = 0.9.dp.toPx())
                    )
                }
            }
        }

        // Unified Branded Center Content across ALL 6 concepts
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = if (isCompact) 16.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val masterLogoRes = if (activeStyle == AppLogoStyle.GLASSMORPHIC) {
                Res.drawable.logo_glassmorphic
            } else {
                Res.drawable.logo_classic
            }

            if (activeMotion == SplashMotionStyle.LIQUID_GLASS) {
                LiquidGlassLogo(
                    logo = masterLogoRes,
                    ending = LiquidGlassEnding.LOGO,
                    height = if (isCompact) 92.dp else 220.dp,
                    backgroundColor = Color.Transparent
                )
            } else {
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
            }

            Spacer(modifier = Modifier.height(if (activeMotion == SplashMotionStyle.LIQUID_GLASS) wordmarkGap * 0.45f else wordmarkGap))

            // Uppercase "D O K U H A"
            Text(
                text = "D O K U H A",
                fontSize = wordmarkFontSize,
                fontWeight = FontWeight.Light,
                color = WordmarkColor,
                letterSpacing = letterSpacingSp,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = wordmarkAlpha.value
                        translationY = wordmarkOffsetY.value
                    }
            )

            Spacer(modifier = Modifier.height(kanjiGap))

            // Kanji Mark: 「 読 破 」
            Text(
                text = "「 読 破 」",
                fontSize = kanjiFontSize,
                fontWeight = FontWeight.Normal,
                color = KanjiColor,
                letterSpacing = if (isCompact) 3.sp else 4.sp,
                modifier = Modifier.alpha(kanjiAlpha.value * 0.85f)
            )

            Spacer(modifier = Modifier.height(formatGap))

            // Styled rotating format badge on the single screen: MANHWA -> MANHUA -> MANGA -> COMIC -> WEBNOVEL
            val currentFormat = SplashFormatEntries[formatIndex % SplashFormatEntries.size]
            Box(
                modifier = Modifier
                    .alpha(formatAlpha.value)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = 0.75.dp,
                        color = currentFormat.accentColor.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(
                        horizontal = if (isCompact) 10.dp else 14.dp,
                        vertical = if (isCompact) 3.dp else 5.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = formatIndex,
                    transitionSpec = {
                        (slideInVertically(tween(240, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(200))) togetherWith
                            (slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { -it / 2 } + fadeOut(tween(160)))
                    },
                    label = "SplashFormatRotation"
                ) { idx ->
                    val entry = SplashFormatEntries[idx % SplashFormatEntries.size]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 4.dp else 5.dp)
                                .clip(CircleShape)
                                .background(entry.accentColor)
                        )
                        Spacer(modifier = Modifier.width(if (isCompact) 6.dp else 8.dp))
                        Text(
                            text = entry.label,
                            fontSize = if (isCompact) 9.sp else 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WordmarkColor.copy(alpha = 0.94f),
                            letterSpacing = if (isCompact) 2.2.sp else 3.2.sp
                        )
                    }
                }
            }
        }

        // Tap to skip hint (only when full-screen & auto-finishing)
        if (autoFinish && !isCompact) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .alpha(wordmarkAlpha.value * 0.45f)
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
}

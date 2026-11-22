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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
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
import panelreader.composeapp.generated.resources.classic_d_frame
import panelreader.composeapp.generated.resources.classic_page_1
import panelreader.composeapp.generated.resources.classic_page_2
import panelreader.composeapp.generated.resources.classic_page_3
import panelreader.composeapp.generated.resources.dokuha_d_frame
import panelreader.composeapp.generated.resources.dokuha_page_1
import panelreader.composeapp.generated.resources.dokuha_page_2
import panelreader.composeapp.generated.resources.dokuha_page_3
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
 * Premium Dokuha Splash Screen with authentic 3D cascading page-turn animation
 * and flowing corner ribbon curves based on the reference design.
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

    // Entrance and scale animatables
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.95f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val wordmarkOffsetY = remember { Animatable(8f) }
    val wordmarkSpacing = remember { Animatable(6f) }

    // Page-turn animation states: Rotation Z (fanning out), Rotation Y (3D lift curl)
    val page1RotZ = remember { Animatable(0f) }
    val page1RotY = remember { Animatable(0f) }
    val page1Shadow = remember { Animatable(0f) }

    val page2RotZ = remember { Animatable(0f) }
    val page2RotY = remember { Animatable(0f) }
    val page2Shadow = remember { Animatable(0f) }

    val page3RotZ = remember { Animatable(0f) }
    val page3RotY = remember { Animatable(0f) }
    val page3Shadow = remember { Animatable(0f) }

    // Breathing motion for background ribbon curves
    val infiniteTransition = rememberInfiniteTransition(label = "ribbon_breathe")
    val ribbonBreathe by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ribbon_breathe"
    )

    // Choreograph the cinematic animation timeline
    LaunchedEffect(Unit) {
        // 1. Entrance of the D frame (0 to 600ms)
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, animationSpec = tween(750, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
        }

        val easeOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
        val easeIn = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)

        // 2. Cascading 3D Page Turn Animation
        // --- Page 1 (Top / Back Page) ---
        launch {
            delay(480)
            // Lift and unfold
            launch { page1Shadow.animateTo(0.45f, tween(380, easing = easeOut)) }
            launch { page1RotY.animateTo(-22f, tween(420, easing = easeOut)) }
            page1RotZ.animateTo(-24f, tween(420, easing = easeOut))

            // Settle smoothly back flush into logo position
            launch { page1Shadow.animateTo(0f, tween(480, easing = easeIn)) }
            launch { page1RotY.animateTo(0f, tween(480, easing = easeIn)) }
            page1RotZ.animateTo(0f, tween(480, easing = easeIn))
        }

        // --- Page 2 (Middle Page) with slight delay ---
        launch {
            delay(640)
            launch { page2Shadow.animateTo(0.40f, tween(380, easing = easeOut)) }
            launch { page2RotY.animateTo(-18f, tween(420, easing = easeOut)) }
            page2RotZ.animateTo(-19f, tween(420, easing = easeOut))

            launch { page2Shadow.animateTo(0f, tween(480, easing = easeIn)) }
            launch { page2RotY.animateTo(0f, tween(480, easing = easeIn)) }
            page2RotZ.animateTo(0f, tween(480, easing = easeIn))
        }

        // --- Page 3 (Bottom / Front Page) cascading flip ---
        launch {
            delay(800)
            launch { page3Shadow.animateTo(0.35f, tween(360, easing = easeOut)) }
            launch { page3RotY.animateTo(-14f, tween(400, easing = easeOut)) }
            page3RotZ.animateTo(-15f, tween(400, easing = easeOut))

            launch { page3Shadow.animateTo(0f, tween(460, easing = easeIn)) }
            launch { page3RotY.animateTo(0f, tween(460, easing = easeIn)) }
            page3RotZ.animateTo(0f, tween(460, easing = easeIn))
        }

        // 3. Wordmark Reveal (1100ms onwards)
        launch {
            delay(1050)
            launch {
                wordmarkAlpha.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
            }
            launch {
                wordmarkOffsetY.animateTo(0f, animationSpec = tween(700, easing = FastOutSlowInEasing))
            }
            launch {
                wordmarkSpacing.animateTo(10f, animationSpec = tween(700, easing = FastOutSlowInEasing))
            }
        }

        // 4. Smooth Transition into App
        delay(2350)
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

            // Subtle radial lighting behind logo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF161619).copy(alpha = 0.85f),
                        Color(0xFF0F0F11).copy(alpha = 0.50f),
                        SplashBackground
                    ),
                    center = center,
                    radius = (w * 0.70f).coerceAtLeast(1f)
                ),
                radius = (w * 0.70f).coerceAtLeast(1f),
                center = center
            )

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
            // Animated Logo Container
            // Shared bottom-left anchor point for the inner page fan-out:
            // In 1024x1024 coordinates, the vertex is at (ox=348, oy=730)
            val anchorOrigin = TransformOrigin(348f / 1024f, 730f / 1024f)
            val logoSize = 188.dp

            val dFrameRes = if (activeStyle == AppLogoStyle.GLASSMORPHIC) {
                Res.drawable.dokuha_d_frame
            } else {
                Res.drawable.classic_d_frame
            }

            val page1Res = if (activeStyle == AppLogoStyle.GLASSMORPHIC) {
                Res.drawable.dokuha_page_1
            } else {
                Res.drawable.classic_page_1
            }

            val page2Res = if (activeStyle == AppLogoStyle.GLASSMORPHIC) {
                Res.drawable.dokuha_page_2
            } else {
                Res.drawable.classic_page_2
            }

            val page3Res = if (activeStyle == AppLogoStyle.GLASSMORPHIC) {
                Res.drawable.dokuha_page_3
            } else {
                Res.drawable.classic_page_3
            }

            Box(
                modifier = Modifier
                    .size(logoSize)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                // Layer 0: The outer D frame (static container with transparent counter)
                Image(
                    painter = painterResource(dFrameRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )

                // Layer 1: Page 1 (Top / Back Page) - 3D unfolding page turn
                Image(
                    painter = painterResource(page1Res),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = anchorOrigin
                            rotationZ = page1RotZ.value
                            rotationY = page1RotY.value
                            cameraDistance = 16f * density
                        }
                )

                // Layer 2: Page 2 (Middle Page)
                Image(
                    painter = painterResource(page2Res),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = anchorOrigin
                            rotationZ = page2RotZ.value
                            rotationY = page2RotY.value
                            cameraDistance = 16f * density
                        }
                )

                // Layer 3: Page 3 (Front / Bottom Page)
                Image(
                    painter = painterResource(page3Res),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = anchorOrigin
                            rotationZ = page3RotZ.value
                            rotationY = page3RotY.value
                            cameraDistance = 16f * density
                        }
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
                modifier = Modifier.alpha(wordmarkAlpha.value * 0.8f)
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

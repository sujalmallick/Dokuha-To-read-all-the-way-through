package ui.screens.webtoonreader

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import core.source.SourcePage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.theme.CapsuleShape
import ui.theme.TextMuted
import ui.theme.TextPrimary
import ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Manga Page Theme Colors — Authentic Japanese Tankōbon aesthetic:
 * Pure ink black, off-white archival manga paper, and multi-density screentone grays.
 */
private val MangaPaperBg = Color(0xFFF9F7F1) // Warm archival off-white paper
private val MangaInkBlack = Color(0xFF0F1115) // Deep Japanese sumi ink
private val MangaInkDark = Color(0xFF1E2128)
private val MangaScreentoneLight = Color(0xFFE2DFD5)
private val MangaScreentoneMid = Color(0xFFB8B4A6)
private val MangaScreentoneDark = Color(0xFF7A776B)
private val MangaGutterColor = Color(0xFFFAF8F3)

/**
 * Authentic Japanese Manga Page Canvas.
 * Renders crisp panel grids, procedural screentone dot textures, radial action speedlines,
 * Japanese sound effects (SFX) typography, speech balloons, and tankōbon folio numbers.
 */
@Composable
fun MangaPageCanvas(
    pageIndex: Int,
    totalPages: Int,
    chapterTitle: String = "Chapter 42",
    isRtl: Boolean = true,
    cropBorders: Boolean = false,
    page: SourcePage? = null,
    modifier: Modifier = Modifier
) {
    val pageType = pageIndex % 4
    val pageNumber = pageIndex + 1

    val imageUrl = page?.imageUrl ?: page?.url
    val isRemoteImage = !imageUrl.isNullOrBlank() &&
        (imageUrl.startsWith("http://", ignoreCase = true) || imageUrl.startsWith("https://", ignoreCase = true))

    if (isRemoteImage) {
        val platformContext = LocalPlatformContext.current
        val imageRequest = remember(imageUrl, page?.headers) {
            ImageRequest.Builder(platformContext)
                .data(imageUrl)
                .apply {
                    val headers = page?.headers
                    if (!headers.isNullOrEmpty()) {
                        val networkHeaders = NetworkHeaders.Builder().apply {
                            headers.forEach { (k, v) -> add(k, v) }
                        }.build()
                        httpHeaders(networkHeaders)
                    }
                }
                .build()
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MangaPaperBg)
                .padding(if (cropBorders) 0.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = "Page $pageNumber",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MangaPaperBg)
                .padding(if (cropBorders) 0.dp else 8.dp)
        ) {
            // Base Panel Layout
            when (pageType) {
                0 -> MangaSplashPageLayout(pageNumber = pageNumber, chapterTitle = chapterTitle)
                1 -> MangaActionTrioLayout(pageNumber = pageNumber)
                2 -> MangaStoryboardGridLayout(pageNumber = pageNumber)
                3 -> MangaNightConfrontationLayout(pageNumber = pageNumber)
            }

            // Spine / Gutter Binding Shadow (Tactile volume curvature)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val isEven = pageNumber % 2 == 0
                        // In RTL Manga: Even pages are on the left (spine is on right); Odd pages on right (spine on left)
                        val shadowBrush = if (isRtl) {
                            if (isEven) {
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color(0x33000000)),
                                    startX = size.width - 24.dp.toPx(),
                                    endX = size.width
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0x33000000), Color.Transparent),
                                    startX = 0f,
                                    endX = 24.dp.toPx()
                                )
                            }
                        } else {
                            // In Western LTR Comic: Odd pages have spine on left, Even on right
                            if (isEven) {
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color(0x33000000)),
                                    startX = size.width - 24.dp.toPx(),
                                    endX = size.width
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0x33000000), Color.Transparent),
                                    startX = 0f,
                                    endX = 24.dp.toPx()
                                )
                            }
                        }

                        onDrawWithContent {
                            drawContent()
                            drawRect(brush = shadowBrush)
                        }
                    }
            )

            // Bottom Folio Indicator (Authentic tankōbon margin)
            MangaFolioIndicator(
                pageNumber = pageNumber,
                totalPages = totalPages,
                chapterTitle = chapterTitle,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Page Variety 0: Epic Hero Climax Splash Page
 * Single full-bleed dynamic action panel with radial focus speedlines, dramatic silhouette,
 * massive Japanese SFX (「ドンッ！！」 DON!), and jagged burst dialogue balloon.
 */
@Composable
private fun MangaSplashPageLayout(
    pageNumber: Int,
    chapterTitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp)
    ) {
        // Full Page Action Canvas with Radial Speedlines & Screentones
        Canvas(modifier = Modifier.fillMaxSize()) {
            val panelRect = Rect(0f, 0f, size.width, size.height)

            // Panel Background Ink Wash
            drawRect(color = Color(0xFFF3EFE6), size = size)

            // Screentone Halftone Pattern in top quadrant
            drawHalftoneScreentone(
                rect = Rect(0f, 0f, size.width, size.height * 0.45f),
                spacing = 10f,
                radius = 1.4f,
                tint = MangaScreentoneMid
            )

            // Radial Speedlines radiating from focal center
            val focalPoint = Offset(size.width * 0.5f, size.height * 0.52f)
            drawRadialSpeedLines(
                center = focalPoint,
                outerRadius = size.maxDimension,
                lineCount = 120,
                color = MangaInkBlack,
                strokeWidth = 2.2f,
                innerRadiusRatio = 0.28f
            )

            // Dynamic Central Character Silhouette
            val heroPath = Path().apply {
                val cx = focalPoint.x
                val cy = focalPoint.y
                moveTo(cx - 30f, cy + 140f)
                lineTo(cx - 20f, cy + 40f)
                lineTo(cx - 55f, cy + 20f)
                lineTo(cx - 45f, cy - 20f)
                lineTo(cx - 25f, cy - 35f)
                // Head & Spiky Hair
                lineTo(cx - 35f, cy - 65f)
                lineTo(cx - 15f, cy - 60f)
                lineTo(cx - 5f, cy - 85f)
                lineTo(cx + 10f, cy - 62f)
                lineTo(cx + 30f, cy - 75f)
                lineTo(cx + 25f, cy - 50f)
                lineTo(cx + 40f, cy - 20f)
                lineTo(cx + 20f, cy + 30f)
                lineTo(cx + 35f, cy + 140f)
                close()
            }
            drawPath(path = heroPath, color = MangaInkBlack)

            // Dramatic Katana / Sword Blade Slash across the page
            drawLine(
                color = Color.White,
                start = Offset(focalPoint.x - 160f, focalPoint.y - 120f),
                end = Offset(focalPoint.x + 200f, focalPoint.y + 160f),
                strokeWidth = 14f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = MangaInkBlack,
                start = Offset(focalPoint.x - 160f, focalPoint.y - 120f),
                end = Offset(focalPoint.x + 200f, focalPoint.y + 160f),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            // Heavy Ink Panel Border
            drawRect(
                color = MangaInkBlack,
                size = size,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Japanese SFX Typography (ドンッ！！ DON!)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ド",
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                color = MangaInkBlack,
                lineHeight = 52.sp
            )
            Text(
                text = "ン",
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                color = MangaInkBlack,
                lineHeight = 58.sp
            )
            Text(
                text = "ッ",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = MangaInkBlack,
                lineHeight = 40.sp
            )
            Box(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(MangaInkBlack)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "*DON!*",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Jagged Climax Speech Burst Balloon
        MangaBurstBalloon(
            text = "I WON'T LET IT END HERE!!\nTHIS IS MY FULL POWER!",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 112.dp)
        )
    }
}

/**
 * Page Variety 1: 3-Panel Dynamic Shonen Action Sequence
 * Top: Opponent's sudden teleport with horizontal dash speedlines.
 * Middle: Extreme cinematic close-up of protagonist's eye with screentone depth and reflection.
 * Bottom: Counter-strike launch with diagonal slash gutter and SFX (「バッ」 BAH!).
 */
@Composable
private fun MangaActionTrioLayout(pageNumber: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // PANEL 1 (Top - 34% height): Enemy arrival with speedlines
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.34f)
                .background(MangaPaperBg)
                .border(2.5.dp, MangaInkBlack)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background Tone
                drawRect(color = Color(0xFFF2ECE1))

                // Horizontal Rush Speedlines
                for (i in 0 until 40) {
                    val y = (i * (size.height / 38))
                    val lineLen = size.width * (0.4f + (i % 5) * 0.12f)
                    drawLine(
                        color = MangaInkBlack,
                        start = Offset(0f, y),
                        end = Offset(lineLen, y),
                        strokeWidth = if (i % 3 == 0) 2.5f else 1.2f
                    )
                }

                // Enemy Silhouette leaping in from right
                val enemyOffset = Offset(size.width * 0.72f, size.height * 0.48f)
                drawCircle(
                    color = MangaInkBlack,
                    radius = 28f,
                    center = enemyOffset
                )
                val capePath = Path().apply {
                    moveTo(enemyOffset.x, enemyOffset.y)
                    lineTo(enemyOffset.x + 90f, enemyOffset.y - 40f)
                    lineTo(enemyOffset.x + 110f, enemyOffset.y + 45f)
                    close()
                }
                drawPath(path = capePath, color = MangaInkBlack)
            }

            // Top Panel Dialogue Balloon
            MangaOvalBalloon(
                text = "He's fast...!\nBehind me?!",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 10.dp)
            )

            // SFX: ZASH! (「ザッ」)
            Text(
                text = "ザッ",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = MangaInkDark,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 6.dp)
            )
        }

        // PANEL 2 (Middle - 28% height): Eye Close-up with Intense Screentone Shading
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.28f)
                .background(MangaInkBlack)
                .border(2.5.dp, MangaInkBlack)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Eye socket highlight
                val eyeCenter = Offset(size.width * 0.5f, size.height * 0.5f)

                // Screentone grid on upper face
                drawHalftoneScreentone(
                    rect = Rect(0f, 0f, size.width, size.height),
                    spacing = 8f,
                    radius = 1.6f,
                    tint = MangaScreentoneDark
                )

                // Intense Eye Cutout
                val eyeWhite = Path().apply {
                    moveTo(eyeCenter.x - 110f, eyeCenter.y)
                    quadraticTo(eyeCenter.x, eyeCenter.y - 38f, eyeCenter.x + 110f, eyeCenter.y)
                    quadraticTo(eyeCenter.x, eyeCenter.y + 36f, eyeCenter.x - 110f, eyeCenter.y)
                    close()
                }
                drawPath(path = eyeWhite, color = Color.White)

                // Iris
                drawCircle(color = MangaInkBlack, radius = 32f, center = eyeCenter)
                // Pupil
                drawCircle(color = Color(0xFF222630), radius = 18f, center = eyeCenter)
                // Catchlight glint
                drawCircle(
                    color = Color.White,
                    radius = 7f,
                    center = Offset(eyeCenter.x - 10f, eyeCenter.y - 10f)
                )

                // Eyebrow furrow lines
                drawLine(
                    color = Color.White,
                    start = Offset(eyeCenter.x - 120f, eyeCenter.y - 48f),
                    end = Offset(eyeCenter.x + 120f, eyeCenter.y - 44f),
                    strokeWidth = 3f
                )
            }

            // Internal Monologue Box (Manga thought caption)
            MangaCaptionBox(
                text = "« I can see the flow of his energy now. »",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 8.dp)
            )
        }

        // PANEL 3 (Bottom - 38% height): Counter-attack launch with dynamic diagonal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.38f)
                .background(MangaPaperBg)
                .border(2.5.dp, MangaInkBlack)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val focal = Offset(size.width * 0.25f, size.height * 0.75f)
                drawRadialSpeedLines(
                    center = focal,
                    outerRadius = size.maxDimension,
                    lineCount = 70,
                    color = MangaInkBlack,
                    strokeWidth = 1.8f,
                    innerRadiusRatio = 0.2f
                )

                // Energy burst arc
                drawArc(
                    color = MangaInkBlack,
                    startAngle = 180f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.1f, size.height * 0.2f),
                    size = Size(size.width * 0.8f, size.height * 0.9f),
                    style = Stroke(width = 6f)
                )
            }

            // SFX: BAH! (「バッ」)
            Text(
                text = "バッ",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = MangaInkBlack,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 20.dp, top = 12.dp)
            )

            MangaOvalBalloon(
                text = "FIRST FORM:\nNIGHT ECLIPSE!",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 12.dp)
            )
        }
    }
}

/**
 * Page Variety 2: 4-Panel Storyboard & Dialogue Grid
 * Classic manga narrative pacing with character speech bubbles, screentone shaded backgrounds,
 * dramatic pause panel with ellipsis, and reaction close-up.
 */
@Composable
private fun MangaStoryboardGridLayout(pageNumber: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // TOP ROW: Two Panels (Right is panel 1, Left is panel 2 in Japanese RTL reading)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.48f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Panel B (Left side in visual, Read second in RTL): Character close-up
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxSize()
                    .background(MangaPaperBg)
                    .border(2.dp, MangaInkBlack)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Halftone Shading on half of panel
                    drawHalftoneScreentone(
                        rect = Rect(0f, 0f, size.width, size.height),
                        spacing = 9f,
                        radius = 1.3f,
                        tint = MangaScreentoneLight
                    )
                    // Crosshatch shadows in corner
                    drawCrosshatch(
                        rect = Rect(0f, size.height * 0.6f, size.width * 0.6f, size.height),
                        lineSpacing = 8f,
                        tint = MangaScreentoneDark
                    )
                }

                MangaOvalBalloon(
                    text = "Then the legend...\nwas true all along?",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(8.dp)
                )
            }

            // Panel A (Right side in visual, Read first in RTL): Ancient Artifact Reveal
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxSize()
                    .background(MangaPaperBg)
                    .border(2.dp, MangaInkBlack)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Pedestal & Glowing Ancient Relic
                    val relicCenter = Offset(size.width * 0.5f, size.height * 0.52f)
                    drawCircle(color = MangaInkBlack, radius = 24f, center = relicCenter)
                    drawCircle(color = Color.White, radius = 16f, center = relicCenter)
                    drawCircle(color = MangaInkBlack, radius = 8f, center = relicCenter)

                    // Glow emission rays
                    drawRadialSpeedLines(
                        center = relicCenter,
                        outerRadius = 90f,
                        lineCount = 24,
                        color = MangaInkBlack,
                        strokeWidth = 1.5f,
                        innerRadiusRatio = 0.45f
                    )
                }

                MangaCaptionBox(
                    text = "THE CIPHER OF AURORA",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )

                Text(
                    text = "ゴオオオ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MangaScreentoneDark,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 4.dp)
                )
            }
        }

        // BOTTOM ROW: Two Panels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.52f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Panel C (Read third in RTL): Dramatic reaction with speedlines
            Box(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxSize()
                    .background(MangaPaperBg)
                    .border(2.dp, MangaInkBlack)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val focal = Offset(size.width * 0.5f, size.height * 0.4f)
                    drawRadialSpeedLines(
                        center = focal,
                        outerRadius = size.maxDimension,
                        lineCount = 45,
                        color = MangaInkBlack,
                        strokeWidth = 1.4f,
                        innerRadiusRatio = 0.35f
                    )
                }

                MangaOvalBalloon(
                    text = "We have less than\none hour before dawn!",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(6.dp)
                )
            }

            // Panel D: Silent Pause Panel with Ellipsis
            Box(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxSize()
                    .background(MangaPaperBg)
                    .border(2.dp, MangaInkBlack)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawHalftoneScreentone(
                        rect = Rect(0f, 0f, size.width, size.height),
                        spacing = 8f,
                        radius = 1.5f,
                        tint = MangaScreentoneMid
                    )
                }

                // Manga Ellipsis Pause Bubble
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, MangaInkBlack, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "• • •",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MangaInkBlack
                    )
                }
            }
        }
    }
}

/**
 * Page Variety 3: Atmospheric Night Confrontation Spread
 * Top: Moonlit sky with screentone clouds, full moon silhouette, castle parapet.
 * Lower: Tense dialogue exchange before battle, ominous rumble SFX (「ゴゴゴゴ」).
 */
@Composable
private fun MangaNightConfrontationLayout(pageNumber: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // TOP PANEL (62% height): Majestic Moonlit Citadel Silhouette
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.62f)
                .background(MangaInkBlack)
                .border(2.5.dp, MangaInkBlack)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Dense Screentone Night Sky
                drawHalftoneScreentone(
                    rect = Rect(0f, 0f, size.width, size.height),
                    spacing = 7f,
                    radius = 2.0f,
                    tint = MangaScreentoneDark
                )

                // Luminous Full Moon
                val moonCenter = Offset(size.width * 0.75f, size.height * 0.28f)
                drawCircle(color = Color.White, radius = 55f, center = moonCenter)

                // Cloud Screentone Bands drifting over moon
                drawLine(
                    color = MangaScreentoneMid,
                    start = Offset(moonCenter.x - 90f, moonCenter.y + 10f),
                    end = Offset(moonCenter.x + 80f, moonCenter.y + 10f),
                    strokeWidth = 12f,
                    cap = StrokeCap.Round
                )

                // Jagged Castle Rooftops Silhouette
                val fortressPath = Path().apply {
                    val base = size.height
                    moveTo(0f, base)
                    lineTo(0f, base * 0.65f)
                    lineTo(size.width * 0.2f, base * 0.65f)
                    // Tower 1
                    lineTo(size.width * 0.2f, base * 0.45f)
                    lineTo(size.width * 0.28f, base * 0.45f)
                    lineTo(size.width * 0.28f, base * 0.68f)
                    // Grand Spire
                    lineTo(size.width * 0.45f, base * 0.68f)
                    lineTo(size.width * 0.50f, base * 0.35f)
                    lineTo(size.width * 0.55f, base * 0.68f)
                    // Wall
                    lineTo(size.width * 0.85f, base * 0.68f)
                    lineTo(size.width * 0.85f, base * 0.50f)
                    lineTo(size.width, base * 0.50f)
                    lineTo(size.width, base)
                    close()
                }
                drawPath(path = fortressPath, color = MangaInkBlack)
            }

            // Ominous Sound Effect: GOGOGOGO (「ゴゴゴゴ」)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 14.dp)
            ) {
                Text(
                    text = "ゴゴゴゴ",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "*RUMBLE OF SHADOWS*",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            MangaCaptionBox(
                text = "Imperial Citadel — Year of the Crimson Eclipse",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 12.dp)
            )
        }

        // BOTTOM PANEL (38% height): Confrontation Dialogue
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.38f)
                .background(MangaPaperBg)
                .border(2.5.dp, MangaInkBlack)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Diagonal confrontation split
                val splitPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width * 0.42f, 0f)
                    lineTo(size.width * 0.35f, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path = splitPath, color = Color(0xFFF0EBE0))
                drawLine(
                    color = MangaInkBlack,
                    start = Offset(size.width * 0.42f, 0f),
                    end = Offset(size.width * 0.35f, size.height),
                    strokeWidth = 2.5f
                )
            }

            MangaOvalBalloon(
                text = "You should have stayed\nin the outer provinces...",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 10.dp)
            )

            MangaBurstBalloon(
                text = "And leave everyone behind? NEVER!",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, bottom = 10.dp)
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// Manga Atmospheric Speech Balloons & Utility Components
// -----------------------------------------------------------------------------------------

/**
 * Classic Manga Oval Speech Balloon with black border and clean typography.
 */
@Composable
private fun MangaOvalBalloon(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.8.dp, MangaInkBlack, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MangaInkBlack,
            lineHeight = 15.sp,
            fontFamily = FontFamily.SansSerif
        )
    }
}

/**
 * Dynamic Spiky Climax Speech Burst Balloon.
 */
@Composable
private fun MangaBurstBalloon(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(2.dp, MangaInkBlack, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = MangaInkBlack,
            lineHeight = 16.sp,
            fontFamily = FontFamily.SansSerif
        )
    }
}

/**
 * Editorial Narrative Caption Box.
 */
@Composable
private fun MangaCaptionBox(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.94f))
            .border(1.2.dp, MangaInkBlack, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MangaInkBlack,
            letterSpacing = 0.5.sp,
            fontStyle = FontStyle.Italic
        )
    }
}

/**
 * Authentic Tankōbon Folio Indicator placed along the bottom margin.
 */
@Composable
private fun MangaFolioIndicator(
    pageNumber: Int,
    totalPages: Int,
    chapterTitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "PANEL TANKŌBON • $chapterTitle",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Medium,
            color = MangaInkDark.copy(alpha = 0.55f),
            letterSpacing = 0.8.sp
        )

        Text(
            text = "— $pageNumber —",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MangaInkDark,
            letterSpacing = 1.2.sp
        )
    }
}

// -----------------------------------------------------------------------------------------
// Procedural Canvas Shaders (Halftone Screentones, Speedlines, Crosshatching)
// -----------------------------------------------------------------------------------------

/**
 * Draws procedural Japanese Halftone Screentone dot patterns on a Canvas DrawScope.
 */
private fun DrawScope.drawHalftoneScreentone(
    rect: Rect,
    spacing: Float,
    radius: Float,
    tint: Color
) {
    var y = rect.top + spacing / 2
    var row = 0
    while (y < rect.bottom) {
        val xOffset = if (row % 2 == 1) spacing / 2 else 0f
        var x = rect.left + spacing / 2 + xOffset
        while (x < rect.right) {
            drawCircle(
                color = tint,
                radius = radius,
                center = Offset(x, y)
            )
            x += spacing
        }
        y += spacing
        row++
    }
}

/**
 * Draws high-impact Radial Focus Speedlines radiating toward or away from an action center.
 */
private fun DrawScope.drawRadialSpeedLines(
    center: Offset,
    outerRadius: Float,
    lineCount: Int,
    color: Color,
    strokeWidth: Float,
    innerRadiusRatio: Float = 0.3f
) {
    val angleStep = (2 * PI / lineCount).toFloat()
    val innerRadius = outerRadius * innerRadiusRatio

    for (i in 0 until lineCount) {
        // Stagger inner radius for organic hand-drawn manga feel
        val jitter = (i % 3 - 1) * (innerRadius * 0.15f)
        val currentInner = innerRadius + jitter
        val angle = i * angleStep

        val startX = center.x + currentInner * cos(angle)
        val startY = center.y + currentInner * sin(angle)
        val endX = center.x + outerRadius * cos(angle)
        val endY = center.y + outerRadius * sin(angle)

        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = if (i % 2 == 0) strokeWidth else strokeWidth * 0.6f
        )
    }
}

/**
 * Draws manga crosshatch ink shading within a given rectangle.
 */
private fun DrawScope.drawCrosshatch(
    rect: Rect,
    lineSpacing: Float,
    tint: Color
) {
    // Diagonal lines /
    var x = rect.left - rect.height
    while (x < rect.right) {
        drawLine(
            color = tint,
            start = Offset(x, rect.bottom),
            end = Offset(x + rect.height, rect.top),
            strokeWidth = 1f
        )
        x += lineSpacing
    }

    // Opposite diagonal lines \
    x = rect.left
    while (x < rect.right + rect.height) {
        drawLine(
            color = tint,
            start = Offset(x, rect.top),
            end = Offset(x - rect.height, rect.bottom),
            strokeWidth = 1f
        )
        x += lineSpacing
    }
}

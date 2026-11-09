package ui.screens.glasslab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ui.glass.GlassTuning
import ui.glass.glassPress
import ui.glass.glassSource
import ui.glass.glassSurface
import ui.glass.rememberPanelGlassState
import ui.theme.CapsuleShape
import ui.theme.ElectricBlue
import ui.theme.GlassBorder
import ui.theme.PanelBackground
import ui.theme.TextPrimary
import ui.theme.TextSecondary

/**
 * Debug-only Glass Lab screen for tuning Apple iOS 26 Liquid Glass optical parameters on real hardware.
 * Reachable from MoreScreen.
 */
class GlassLabScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val glassState = rememberPanelGlassState()

        // Live Lens Tuning States
        var frostDp by remember { mutableFloatStateOf(1.0f) }
        var refraction by remember { mutableFloatStateOf(0.60f) }
        var curve by remember { mutableFloatStateOf(0.60f) }
        var edge by remember { mutableFloatStateOf(0.15f) }
        var dispersion by remember { mutableFloatStateOf(0.15f) }
        var saturation by remember { mutableFloatStateOf(1.15f) }
        var contrast by remember { mutableFloatStateOf(1.00f) }
        var tintAlpha by remember { mutableFloatStateOf(0.06f) }
        var isDarkTint by remember { mutableStateOf(false) }

        val currentTuning = remember(
            frostDp, refraction, curve, edge, dispersion, saturation, contrast, tintAlpha, isDarkTint
        ) {
            val baseColor = if (isDarkTint) Color.Black else Color.White
            GlassTuning(
                frost = frostDp.dp,
                refraction = refraction,
                curve = curve,
                edge = edge,
                dispersion = dispersion,
                saturation = saturation,
                contrast = contrast,
                tint = baseColor.copy(alpha = tintAlpha)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PanelBackground)
        ) {
            // LAYER 1: Colorful Background Source Layer (sampled by glass overlays)
            // Marked with glassSource
            ColorfulBackdrop(
                modifier = Modifier
                    .fillMaxSize()
                    .glassSource(glassState)
            )

            // LAYER 2: Glass Overlays & Interactive Tuners
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                // Top Bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navigator.pop() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .glassSurface(glassState, tuning = currentTuning, shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Liquid Glass Lab",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Apple iOS 26 Lens Refraction Tuner",
                                fontSize = 12.sp,
                                color = ElectricBlue
                            )
                        }
                    }
                }

                // 3 Live Sample Glass Surfaces
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "LIVE SAMPLES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        // Sample 1: Circular Action Button (CircleShape)
                        val circleInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .glassPress(circleInteraction)
                                .glassSurface(glassState, shape = CircleShape, tuning = currentTuning)
                                .clickable(interactionSource = circleInteraction, indication = null) { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = ElectricBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Sample 2: Floating Capsule Bar (CapsuleShape)
                        val capsuleInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(48.dp)
                                .glassPress(capsuleInteraction)
                                .glassSurface(glassState, shape = CapsuleShape, tuning = currentTuning)
                                .clickable(interactionSource = capsuleInteraction, indication = null) { }
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Floating Capsule Lens",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Sample 3: Large Modal Sheet Surface (RoundedCornerShape 28dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassSurface(
                                    glassState,
                                    shape = RoundedCornerShape(28.dp),
                                    tuning = currentTuning
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Continuous Curved Sheet (28dp)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Refracts and disperses the colorful backdrop along edges while maintaining high optical clarity in the center. Notice the specular rim lighting.",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // Controls Panel
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "LENS CONTROLS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue,
                            letterSpacing = 1.sp
                        )

                        // 1. Frost
                        SliderRow(
                            label = "Frost (Blur)",
                            valueText = "${(frostDp * 10).toInt() / 10f} dp",
                            value = frostDp,
                            range = 0f..5f,
                            onValueChange = { frostDp = it }
                        )

                        // 2. Refraction
                        SliderRow(
                            label = "Refraction",
                            valueText = "${(refraction * 100).toInt() / 100f}",
                            value = refraction,
                            range = 0f..1.5f,
                            onValueChange = { refraction = it }
                        )

                        // 3. Curve
                        SliderRow(
                            label = "Curve",
                            valueText = "${(curve * 100).toInt() / 100f}",
                            value = curve,
                            range = 0f..1.5f,
                            onValueChange = { curve = it }
                        )

                        // 4. Edge (Rim Lighting)
                        SliderRow(
                            label = "Edge (Rim Light)",
                            valueText = "${(edge * 100).toInt() / 100f}",
                            value = edge,
                            range = 0f..0.5f,
                            onValueChange = { edge = it }
                        )

                        // 5. Dispersion (Chromatic)
                        SliderRow(
                            label = "Dispersion (RGB)",
                            valueText = "${(dispersion * 100).toInt() / 100f}",
                            value = dispersion,
                            range = 0f..0.5f,
                            onValueChange = { dispersion = it }
                        )

                        // 6. Saturation
                        SliderRow(
                            label = "Saturation",
                            valueText = "${(saturation * 100).toInt() / 100f}",
                            value = saturation,
                            range = 0.5f..2.5f,
                            onValueChange = { saturation = it }
                        )

                        // 7. Contrast
                        SliderRow(
                            label = "Contrast",
                            valueText = "${(contrast * 100).toInt() / 100f}",
                            value = contrast,
                            range = 0.5f..2.0f,
                            onValueChange = { contrast = it }
                        )

                        // 8. Tint Alpha & Base Color
                        SliderRow(
                            label = if (isDarkTint) "Dark Tint Alpha" else "Clear Tint Alpha",
                            valueText = "${(tintAlpha * 100).toInt()}%",
                            value = tintAlpha,
                            range = 0f..0.5f,
                            onValueChange = { tintAlpha = it }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tint Base Color",
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Button(
                                onClick = {
                                    isDarkTint = !isDarkTint
                                    if (isDarkTint && tintAlpha < 0.15f) tintAlpha = 0.18f
                                    if (!isDarkTint && tintAlpha > 0.15f) tintAlpha = 0.06f
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkTint) Color.DarkGray else ElectricBlue
                                ),
                                shape = CapsuleShape
                            ) {
                                Text(
                                    text = if (isDarkTint) "Dark (18%)" else "Clear (6%)",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Print / Copy Values Button
                        Button(
                            onClick = {
                                val logMessage = """
                                    // ===== FROZEN GLASS TUNING =====
                                    GlassTuning(
                                        frost = ${frostDp}f.dp,
                                        refraction = ${refraction}f,
                                        curve = ${curve}f,
                                        edge = ${edge}f,
                                        dispersion = ${dispersion}f,
                                        saturation = ${saturation}f,
                                        contrast = ${contrast}f,
                                        tint = ${if (isDarkTint) "Color.Black" else "Color.White"}.copy(alpha = ${tintAlpha}f)
                                    )
                                """.trimIndent()
                                println(logMessage)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = CapsuleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Text(
                                text = "Print Values to Log",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = TextPrimary
            )
            Text(
                text = valueText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = ElectricBlue
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = ElectricBlue,
                activeTrackColor = ElectricBlue,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

/**
 * High-contrast, multi-hued backdrop designed to reveal lens curvature, chromatic dispersion,
 * edge lighting, and refraction.
 */
@Composable
private fun ColorfulBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // Deep background gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F172A),
                    Color(0xFF0B0F19),
                    Color(0xFF020617)
                )
            )
        )

        // Vibrant geometric elements that provide sharp edges to refract
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF2A85), Color.Transparent),
                center = Offset(size.width * 0.25f, size.height * 0.2f),
                radius = size.width * 0.45f
            ),
            center = Offset(size.width * 0.25f, size.height * 0.2f),
            radius = size.width * 0.45f
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF007AFF), Color.Transparent),
                center = Offset(size.width * 0.8f, size.height * 0.35f),
                radius = size.width * 0.5f
            ),
            center = Offset(size.width * 0.8f, size.height * 0.35f),
            radius = size.width * 0.5f
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF00E5FF), Color.Transparent),
                center = Offset(size.width * 0.3f, size.height * 0.65f),
                radius = size.width * 0.4f
            ),
            center = Offset(size.width * 0.3f, size.height * 0.65f),
            radius = size.width * 0.4f
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF9500), Color.Transparent),
                center = Offset(size.width * 0.75f, size.height * 0.8f),
                radius = size.width * 0.45f
            ),
            center = Offset(size.width * 0.75f, size.height * 0.8f),
            radius = size.width * 0.45f
        )
    }
}

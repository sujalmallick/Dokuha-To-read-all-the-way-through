package ui.screens.webtoonreader

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.components.GlassSegmentedControl
import ui.components.PanelBottomNavBar
import ui.components.PanelSlider
import ui.components.PanelSwitch
import ui.components.PanelTab
import ui.glass.GlassVariant
import ui.glass.glassDimming
import ui.glass.glassSource
import ui.glass.glassSurface
import ui.glass.rememberPanelGlassState
import ui.theme.CapsuleShape
import ui.theme.PanelBackground
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary

enum class ReadingMode(val label: String) {
    WEBTOON("Webtoon"),
    RTL("RTL"),
    LTR("LTR"),
    VERTICAL("Vertical")
}

class ReaderSettingsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        ReaderSettingsScreenContent(
            onDismiss = { navigator.pop() },
            onTabSelected = { navigator.pop() }
        )
    }
}

@Composable
fun ReaderSettingsScreenContent(
    onDismiss: () -> Unit,
    onTabSelected: (PanelTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    var selectedMode by remember { mutableStateOf(ReadingMode.WEBTOON) }
    var brightness by remember { mutableFloatStateOf(0.80f) }
    var sidePadding by remember { mutableFloatStateOf(0.12f) }
    var cropBorders by remember { mutableStateOf(true) }

    val sheetShape = RoundedCornerShape(28.dp)
    val readingModes = listOf(ReadingMode.WEBTOON, ReadingMode.RTL, ReadingMode.LTR, ReadingMode.VERTICAL)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        // LAYER 1: Webtoon background canvas (Sampled Source Layer)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassSource(glassState)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            WebtoonPanelSlice(
                sliceIndex = 1,
                totalSlices = 4,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Ambient backdrop dark scrim for modal focus
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f))
        )

        // LAYER 2: Functional Overlays (Liquid Glass Bottom Sheet & Floating Dock)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            // Reading Settings Liquid Glass Modal Bottom Sheet
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .glassDimming(alpha = 0.30f, shape = sheetShape)
                    .glassSurface(
                        state = glassState,
                        variant = GlassVariant.Regular,
                        shape = sheetShape
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    // Drag Handle affordance at top center
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CapsuleShape)
                            .background(Color.White.copy(alpha = 0.35f))
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = "Reading Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Reading Mode Section
                    Text(
                        text = "Reading Mode",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Unified Glass Segmented Control with sliding lens indicator
                    GlassSegmentedControl(
                        items = readingModes,
                        selectedIndex = readingModes.indexOf(selectedMode),
                        onItemSelected = { selectedMode = readingModes[it] },
                        labelProvider = { it.label },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Brightness Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = "Brightness",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Brightness",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "${(brightness * 100).toInt()}%",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        PanelSlider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Side Padding Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Side Padding",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(sidePadding * 100).toInt()}%",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        PanelSlider(
                            value = sidePadding,
                            onValueChange = { sidePadding = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Crop Borders Toggle Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Crop borders",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        PanelSwitch(
                            checked = cropBorders,
                            onCheckedChange = { cropBorders = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Navigation Bar visible underneath as floating Liquid Glass dock
            PanelBottomNavBar(
                selectedTab = PanelTab.LIBRARY,
                onTabSelected = onTabSelected,
                glassState = glassState,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Preview
@Composable
fun ReaderSettingsScreenPreview() {
    PanelTheme {
        ReaderSettingsScreenContent(
            onDismiss = {},
            onTabSelected = {}
        )
    }
}

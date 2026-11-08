package ui.screens.webtoonreader

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.TableRows
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.components.GlassContainer
import ui.components.PanelBottomNavBar
import ui.components.PanelSlider
import ui.components.PanelSwitch
import ui.components.PanelTab
import ui.components.ReaderModeTile
import ui.theme.BottomSheetShape
import ui.theme.CapsuleShape
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelBackground
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.PanelTheme
import ui.theme.TextPrimary
import ui.theme.TextSecondary

enum class ReadingMode(val label: String) {
    WEBTOON("Webtoon"),
    RTL("Right to Left"),
    LTR("Left to Right"),
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
    val hazeState = remember { HazeState() }
    var selectedMode by remember { mutableStateOf(ReadingMode.WEBTOON) }
    var brightness by remember { mutableFloatStateOf(0.80f) }
    var sidePadding by remember { mutableFloatStateOf(0.12f) }
    var cropBorders by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        // Blurred Webtoon reader background canvas
        WebtoonPanelSlice(
            sliceIndex = 1,
            totalSlices = 4,
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // Semi-transparent dark scrim overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        // Reading Settings Modal Bottom Sheet (Screen 5)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            GlassContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = BottomSheetShape,
                backgroundColor = PanelSurfaceGlassHigh.copy(alpha = 0.92f),
                borderColor = GlassBorderStrong,
                hazeState = hazeState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Drag Handle pill at top center
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CapsuleShape)
                            .background(Color.White.copy(alpha = 0.3f))
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Reading Mode Section
                    Text(
                        text = "Reading Mode",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4 Mode Toggle Tiles in a Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReaderModeTile(
                            title = "Webtoon",
                            icon = Icons.Default.StayCurrentPortrait,
                            isSelected = selectedMode == ReadingMode.WEBTOON,
                            onClick = { selectedMode = ReadingMode.WEBTOON },
                            modifier = Modifier.weight(1f)
                        )
                        ReaderModeTile(
                            title = "Right to Left",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            isSelected = selectedMode == ReadingMode.RTL,
                            onClick = { selectedMode = ReadingMode.RTL },
                            modifier = Modifier.weight(1f)
                        )
                        ReaderModeTile(
                            title = "Left to Right",
                            icon = Icons.Default.AutoStories,
                            isSelected = selectedMode == ReadingMode.LTR,
                            onClick = { selectedMode = ReadingMode.LTR },
                            modifier = Modifier.weight(1f)
                        )
                        ReaderModeTile(
                            title = "Vertical",
                            icon = Icons.Default.TableRows,
                            isSelected = selectedMode == ReadingMode.VERTICAL,
                            onClick = { selectedMode = ReadingMode.VERTICAL },
                            modifier = Modifier.weight(1f)
                        )
                    }

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

            // Bottom Navigation Bar visible underneath
            PanelBottomNavBar(
                selectedTab = PanelTab.LIBRARY,
                onTabSelected = onTabSelected,
                hazeState = hazeState
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

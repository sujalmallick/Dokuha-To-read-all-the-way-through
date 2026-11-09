package ui.components

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.glass.GlassVariant
import ui.glass.PanelGlassState
import ui.glass.glassPress
import ui.glass.glassSurface
import ui.motion.rememberTabIndicatorSpring
import ui.theme.CapsuleShape
import ui.theme.ElectricBlue
import ui.theme.GlassBorder
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.PanelTheme
import ui.theme.TextSecondary

enum class PanelTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    LIBRARY("Library", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    UPDATES("Updates", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    BROWSE("Browse", Icons.Filled.Explore, Icons.Outlined.Explore),
    MORE("More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
}

/**
 * Floating Liquid Glass capsule dock navigation bar.
 * Features:
 * - Liquid glass lens body with edge refraction rim
 * - Sliding glass lens indicator moving between tabs with a no-overshoot spring
 * - Gentle swell press feedback (~1.04)
 * - Flat ElectricBlue accent, no gradient slop
 */
@Composable
fun PanelBottomNavBar(
    selectedTab: PanelTab,
    onTabSelected: (PanelTab) -> Unit,
    modifier: Modifier = Modifier,
    glassState: PanelGlassState? = null
) {
    val dockShape = CapsuleShape

    val surfaceModifier = when {
        glassState != null -> {
            Modifier.glassSurface(
                state = glassState,
                variant = GlassVariant.Regular,
                shape = dockShape
            )
        }
        else -> {
            Modifier
                .clip(dockShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, GlassBorder, dockShape)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .then(surfaceModifier),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val tabCount = PanelTab.entries.size
            val tabWidth = maxWidth / tabCount
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedTab.ordinal,
                animationSpec = rememberTabIndicatorSpring(),
                label = "tabIndicatorOffset"
            )

            // Sliding Glass Lens Indicator
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(CapsuleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.28f), CapsuleShape)
            )

            // Tab items row
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PanelTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val contentColor = if (isSelected) ElectricBlue else TextSecondary
                    val interactionSource = remember { MutableInteractionSource() }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .glassPress(interactionSource = interactionSource, pressedScale = 1.04f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onTabSelected(tab) }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.title,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = tab.title,
                            color = contentColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            letterSpacing = (-0.2).sp
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PanelBottomNavBarPreview() {
    PanelTheme {
        PanelBottomNavBar(
            selectedTab = PanelTab.LIBRARY,
            onTabSelected = {}
        )
    }
}

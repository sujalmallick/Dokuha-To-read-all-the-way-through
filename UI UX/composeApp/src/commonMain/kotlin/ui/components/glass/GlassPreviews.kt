package ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.glass.GlassQualitySetting
import ui.theme.LocalGlassTokens
import ui.theme.PanelTheme
import ui.theme.ThemeMode

@Composable
fun GlassDesignSystemGallery(
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    var selectedSegment by remember { mutableStateOf(GlassQualitySetting.AUTO) }
    var searchQuery by remember { mutableStateOf("") }
    var sliderValue by remember { mutableFloatStateOf(0.45f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassTopBar(
            title = "Library",
            onBack = {},
            trailingActions = {
                GlassIconButton(
                    icon = Icons.Default.Search,
                    contentDescription = "Search",
                    onClick = {}
                )
            }
        )

        GlassSearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Search titles..."
        )

        GlassSegmentedControl(
            options = GlassQualitySetting.entries,
            selected = selectedSegment,
            onSelect = { selectedSegment = it },
            labelOf = { it.label }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassChip(label = "Unread", selected = true, onClick = {})
            GlassChip(label = "Downloaded", selected = false, onClick = {})
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Glass Card Title",
                    color = tokens.textOnGlass,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Readable contrast over bright, dark, and busy covers.",
                    color = tokens.subtextOnGlass,
                    fontSize = 13.sp
                )
                GlassSlider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassButton(
                text = "Read",
                icon = Icons.Default.Bookmark,
                variant = GlassButtonVariant.PRIMARY,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "Download",
                icon = Icons.Default.Download,
                variant = GlassButtonVariant.GLASS,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }

        GlassBottomDock {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Library", color = tokens.textOnGlass, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("Updates", color = tokens.subtextOnGlass, maxLines = 1)
                Text("Browse", color = tokens.subtextOnGlass, maxLines = 1)
                Text("More", color = tokens.subtextOnGlass, maxLines = 1)
            }
        }
    }
}

@Preview
@Composable
fun GlassDesignSystemDarkPreview() {
    PanelTheme(themeMode = ThemeMode.DARK) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
            )
        ) {
            GlassDesignSystemGallery()
        }
    }
}

@Preview
@Composable
fun GlassDesignSystemLightPreview() {
    PanelTheme(themeMode = ThemeMode.SYSTEM, darkTheme = false) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0)))
            )
        ) {
            GlassDesignSystemGallery()
        }
    }
}

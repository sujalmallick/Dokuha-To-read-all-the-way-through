package ui.screens.extensions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.extension.DefaultExtensionRepository
import data.extension.ExtensionItem
import data.extension.ExtensionRepository
import data.extension.ExtensionStatus
import data.extension.ExtensionTrustLevel
import data.mock.MockExtension
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.GlassSegmentedControl
import ui.components.PanelBottomNavBar
import ui.components.PanelTab
import ui.glass.glassPress
import ui.glass.glassSource
import ui.glass.rememberPanelGlassState
import ui.screens.browse.BrowseScreen
import ui.screens.library.LibraryScreen
import ui.screens.more.MoreScreen
import ui.screens.updates.UpdatesScreen
import ui.theme.CapsuleShape
import ui.theme.GlassBorder
import ui.theme.PanelBackground
import ui.theme.PanelTheme
import ui.theme.TextMuted
import ui.theme.TextPrimary
import ui.theme.TextSecondary
import ui.theme.TextTertiary

class ExtensionsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: ExtensionRepository = koinInject()

        ExtensionsScreenContent(
            extensionRepository = repository,
            onTabSelected = { tab ->
                when (tab) {
                    PanelTab.LIBRARY -> navigator.push(LibraryScreen())
                    PanelTab.UPDATES -> navigator.push(UpdatesScreen())
                    PanelTab.BROWSE -> navigator.push(BrowseScreen())
                    PanelTab.MORE -> navigator.push(MoreScreen())
                }
            }
        )
    }
}

@Composable
fun ExtensionsScreenContent(
    initialExtensions: List<MockExtension> = emptyList(),
    extensionRepository: ExtensionRepository? = null,
    onTabSelected: (PanelTab) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val repository = remember(extensionRepository) {
        extensionRepository ?: DefaultExtensionRepository()
    }

    val glassState = rememberPanelGlassState()
    val extensions by repository.extensions.collectAsState()
    val selectedLang by repository.selectedLanguage.collectAsState()
    val searchQuery by repository.searchQuery.collectAsState()

    var selectedStatusTab by remember { mutableStateOf("Installed") }
    var selectedExtensionForSheet by remember { mutableStateOf<ExtensionItem?>(null) }
    var isAddRepoVisible by remember { mutableStateOf(false) }

    val languages = listOf("ALL", "EN", "JA", "KO", "ES", "FR")

    // Filter by tab, language, and search query
    val installedCount = extensions.count { it.status == ExtensionStatus.INSTALLED || it.status == ExtensionStatus.UPDATE_AVAILABLE }
    val updatesCount = extensions.count { it.status == ExtensionStatus.UPDATE_AVAILABLE }
    val availableCount = extensions.count { it.status == ExtensionStatus.AVAILABLE || it.status == ExtensionStatus.INSTALLING }

    val statusTabs = listOf(
        "Installed ($installedCount)",
        "Updates ($updatesCount)",
        "Available ($availableCount)"
    )

    val currentTabIndex = when (selectedStatusTab) {
        "Updates" -> 1
        "Available" -> 2
        else -> 0
    }

    val filteredExtensions = extensions.filter { ext ->
        // Status filter
        val matchesStatus = when (selectedStatusTab) {
            "Installed" -> ext.status == ExtensionStatus.INSTALLED || ext.status == ExtensionStatus.UPDATE_AVAILABLE
            "Updates" -> ext.status == ExtensionStatus.UPDATE_AVAILABLE
            "Available" -> ext.status == ExtensionStatus.AVAILABLE || ext.status == ExtensionStatus.INSTALLING
            else -> true
        }

        // Language filter
        val matchesLang = if (selectedLang == "ALL") true else ext.lang.equals(selectedLang, ignoreCase = true) || ext.lang == "ALL"

        // Search query
        val matchesSearch = if (searchQuery.isBlank()) true else {
            ext.name.contains(searchQuery, ignoreCase = true) ||
                    ext.sources.any { it.name.contains(searchQuery, ignoreCase = true) } ||
                    ext.pkgName.contains(searchQuery, ignoreCase = true)
        }

        matchesStatus && matchesLang && matchesSearch
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        // Ambient Liquid glow gradients
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.25f),
                    radius = size.width * 0.65f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFA855F7).copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.55f),
                    radius = size.width * 0.60f
                )
            )
        }

        // Main Scrollable Content Layer
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .glassSource(glassState),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 110.dp
            )
        ) {
            // Header Bar
            item {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Extensions",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp
                        )

                        // Add Repo Pill Button
                        Box(
                            modifier = Modifier
                                .clip(CapsuleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), CapsuleShape)
                                .clickable { isAddRepoVisible = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Repo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Search Field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search extensions or sources...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { repository.setSearchQuery(it) },
                                    textStyle = TextStyle(
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    ),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { repository.setSearchQuery("") }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Glass Segmented Status Tabs
                    GlassSegmentedControl(
                        items = statusTabs,
                        selectedIndex = currentTabIndex,
                        onItemSelected = { index ->
                            selectedStatusTab = when (index) {
                                1 -> "Updates"
                                2 -> "Available"
                                else -> "Installed"
                            }
                        },
                        labelProvider = { it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Language Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(languages) { lang ->
                            val isSelected = selectedLang == lang
                            Box(
                                modifier = Modifier
                                    .clip(CapsuleShape)
                                    .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.06f))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.White else Color.White.copy(alpha = 0.12f),
                                        CapsuleShape
                                    )
                                    .clickable { repository.setLanguageFilter(lang) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lang,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF090D15) else TextSecondary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Extension Items List
            if (filteredExtensions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No extensions match \"$searchQuery\""
                            else "No extensions found in this section",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                items(filteredExtensions, key = { it.id }) { ext ->
                    ExtensionCardItem(
                        extension = ext,
                        onCardClick = { selectedExtensionForSheet = ext },
                        onInstallClick = { repository.installExtension(ext.id) },
                        onUpdateClick = { repository.updateExtension(ext.id) },
                        onSettingsClick = { selectedExtensionForSheet = ext }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // LAYER 2: Floating Liquid Glass Bottom Navigation Dock
        PanelBottomNavBar(
            selectedTab = PanelTab.MORE,
            onTabSelected = onTabSelected,
            glassState = glassState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )

        // LAYER 3: Extension Details Sheet Modal
        ExtensionDetailSheet(
            extension = selectedExtensionForSheet,
            isVisible = selectedExtensionForSheet != null,
            onDismiss = { selectedExtensionForSheet = null },
            onInstall = { ext ->
                repository.installExtension(ext.id)
                selectedExtensionForSheet = null
            },
            onUpdate = { ext ->
                repository.updateExtension(ext.id)
                selectedExtensionForSheet = null
            },
            onUninstall = { ext ->
                repository.uninstallExtension(ext.id)
                selectedExtensionForSheet = null
            },
            onClearCache = { ext ->
                repository.clearExtensionCache(ext.id)
            }
        )

        // LAYER 4: Add Repository Dialog
        AddRepoDialog(
            isVisible = isAddRepoVisible,
            onDismiss = { isAddRepoVisible = false },
            onAddRepo = { url, name ->
                repository.addRepository(url, name)
            }
        )
    }
}

@Composable
private fun ExtensionCardItem(
    extension: ExtensionItem,
    onCardClick: () -> Unit,
    onInstallClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)
    val brandColor = Color(extension.iconColorHex)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassPress(interactionSource)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.03f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.16f)
                    )
                ),
                shape = shape
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onCardClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Extension icon avatar in liquid glass frame
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                brandColor.copy(alpha = 0.22f),
                                brandColor.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                brandColor.copy(alpha = 0.30f)
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = extension.name.take(2).uppercase(),
                    color = brandColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title, Version, Language & Trust Badges
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = extension.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Language Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = extension.lang,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "v${extension.versionName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    // Trust indicator icon
                    Spacer(modifier = Modifier.width(6.dp))
                    when (extension.trustLevel) {
                        ExtensionTrustLevel.OFFICIAL -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Official",
                                tint = Color(0xFF00DC64),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        ExtensionTrustLevel.COMMUNITY -> {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Community",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        ExtensionTrustLevel.UNTRUSTED -> {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Untrusted",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Action Button
            when (extension.status) {
                ExtensionStatus.UPDATE_AVAILABLE -> {
                    val updateInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .clip(CapsuleShape)
                            .glassPress(updateInteraction)
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.35f), CapsuleShape)
                            .clickable(
                                interactionSource = updateInteraction,
                                indication = null,
                                onClick = onUpdateClick
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Update",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                ExtensionStatus.AVAILABLE -> {
                    val installInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .clip(CapsuleShape)
                            .glassPress(installInteraction)
                            .background(Color.White)
                            .clickable(
                                interactionSource = installInteraction,
                                indication = null,
                                onClick = onInstallClick
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Install",
                            color = Color(0xFF090D15),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                ExtensionStatus.INSTALLING -> {
                    Box(
                        modifier = Modifier
                            .clip(CapsuleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${(extension.installProgress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                ExtensionStatus.INSTALLED -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CapsuleShape)
                            .clickable(onClick = onSettingsClick)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Installed",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                ExtensionStatus.UNINSTALLING -> {
                    Text(
                        text = "Removing...",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ExtensionsScreenPreview() {
    PanelTheme {
        ExtensionsScreenContent()
    }
}

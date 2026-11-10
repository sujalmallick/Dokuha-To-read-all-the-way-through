package ui.screens.extensions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.mock.FakePanelRepository
import data.mock.MockExtension
import data.mock.PanelRepository
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.ExtensionItemCard
import ui.components.GlassSegmentedControl
import ui.components.PanelBottomNavBar
import ui.components.PanelTab
import ui.glass.glassSource
import ui.glass.rememberPanelGlassState
import ui.screens.browse.BrowseScreen
import ui.screens.library.LibraryScreen
import ui.screens.more.MoreScreen
import ui.screens.updates.UpdatesScreen
import ui.theme.PanelBackground
import ui.theme.PanelTheme
import ui.theme.TextPrimary

import androidx.compose.runtime.rememberCoroutineScope
import core.extensions.ExtensionManager
import kotlinx.coroutines.launch

class ExtensionsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()
        val extensionManager: ExtensionManager = koinInject()
        val coroutineScope = rememberCoroutineScope()

        val mockExtensions by repository.getExtensions().collectAsState(initial = emptyList())
        val installedExts by extensionManager.installedExtensions.collectAsState(initial = emptyList())
        val availableCatalog by extensionManager.availableExtensions.collectAsState(initial = emptyList())

        val effectiveExtensions = remember(mockExtensions, installedExts, availableCatalog) {
            if (installedExts.isNotEmpty() || availableCatalog.isNotEmpty()) {
                val installedIds = installedExts.map { it.id }.toSet()
                val list = mutableListOf<MockExtension>()
                for (inst in installedExts) {
                    list.add(
                        MockExtension(
                            id = inst.id,
                            name = inst.name,
                            version = inst.version,
                            lang = inst.lang.uppercase(),
                            isInstalled = true,
                            hasUpdate = false,
                            iconUrl = inst.iconUrl
                        )
                    )
                }
                for (avail in availableCatalog) {
                    if (!installedIds.contains(avail.id)) {
                        list.add(
                            MockExtension(
                                id = avail.id,
                                name = avail.name,
                                version = avail.version,
                                lang = avail.lang.uppercase(),
                                isInstalled = false,
                                hasUpdate = false,
                                iconUrl = avail.iconUrl
                            )
                        )
                    }
                }
                list
            } else {
                mockExtensions
            }
        }

        ExtensionsScreenContent(
            initialExtensions = effectiveExtensions,
            onActionClick = { ext ->
                coroutineScope.launch {
                    if (ext.isInstalled) {
                        extensionManager.uninstallExtension(ext.id)
                    } else {
                        val catalogItem = availableCatalog.firstOrNull { it.id == ext.id }
                        if (catalogItem != null) {
                            val repoBase = extensionManager.repoUrls.value.firstOrNull() ?: ""
                            extensionManager.installExtension(catalogItem, repoBase)
                        }
                    }
                }
            },
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
    initialExtensions: List<MockExtension>,
    onTabSelected: (PanelTab) -> Unit,
    onActionClick: (MockExtension) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    var selectedFilter by remember { mutableStateOf("Installed") }
    val filters = listOf("Installed", "Available", "Updates")

    // State list for interactive updates
    val extensionsList = remember(initialExtensions) {
        mutableStateListOf<MockExtension>().apply {
            addAll(
                if (initialExtensions.isNotEmpty()) initialExtensions
                else FakePanelRepository().getExtensions().let { emptyList() }
            )
        }
    }

    val filteredExtensions = extensionsList.filter { ext ->
        when (selectedFilter) {
            "Installed" -> ext.isInstalled
            "Available" -> !ext.isInstalled
            "Updates" -> ext.hasUpdate
            else -> true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        // Ambient liquid refraction glow orbs in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.30f),
                    radius = size.width * 0.65f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFA855F7).copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.60f),
                    radius = size.width * 0.60f
                )
            )
        }

        // LAYER 1: Scrollable Content Layer (marked with glassSource)
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
            // Header Bar & Glass Segmented Control
            item {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Text(
                        text = "Extensions",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Unified Glass Segmented Control with sliding glass lens indicator
                    GlassSegmentedControl(
                        items = filters,
                        selectedIndex = filters.indexOf(selectedFilter),
                        onItemSelected = { selectedFilter = filters[it] },
                        labelProvider = { it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Extensions Items List (Quiet non-glass content layer)
            items(filteredExtensions) { ext ->
                val avatarColor = when (ext.name) {
                    "MangaDex" -> Color(0xFFFF6740)
                    "Comick" -> Color(0xFF60A5FA)
                    "Asura Scans" -> Color(0xFFA855F7)
                    "WebNovel" -> Color(0xFFEC4899)
                    else -> Color(0xFF38BDF8)
                }

                ExtensionItemCard(
                    name = ext.name,
                    version = ext.version,
                    isInstalled = ext.isInstalled,
                    hasUpdate = ext.hasUpdate,
                    avatarColor = avatarColor,
                    onActionClick = {
                        val index = extensionsList.indexOfFirst { it.id == ext.id }
                        if (index != -1) {
                            val current = extensionsList[index]
                            extensionsList[index] = current.copy(
                                isInstalled = !current.isInstalled,
                                hasUpdate = false
                            )
                        }
                        onActionClick(ext)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
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
    }
}

@Preview
@Composable
fun ExtensionsScreenPreview() {
    PanelTheme {
        ExtensionsScreenContent(
            initialExtensions = emptyList(),
            onTabSelected = {}
        )
    }
}

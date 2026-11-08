package ui.screens.extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.CategoryPillRow
import ui.components.ExtensionItemCard
import ui.components.PanelBottomNavBar
import ui.components.PanelTab
import ui.screens.browse.BrowseScreen
import ui.screens.library.LibraryScreen
import ui.screens.more.MoreScreen
import ui.screens.updates.UpdatesScreen
import ui.theme.PanelBackground
import ui.theme.PanelTheme
import ui.theme.TextPrimary

class ExtensionsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()

        val initialExtensions by repository.getExtensions().collectAsState(initial = emptyList())

        ExtensionsScreenContent(
            initialExtensions = initialExtensions,
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
    modifier: Modifier = Modifier
) {
    val hazeState = remember { HazeState() }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 100.dp
            )
        ) {
            // Header Bar & Filter Tabs
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

                    // Status Pill Tabs
                    CategoryPillRow(
                        categories = filters,
                        selectedCategory = selectedFilter,
                        onCategorySelected = { selectedFilter = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Extensions Items List matching option 2.png Screen 8
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
                                isInstalled = true,
                                hasUpdate = false
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Floating Frosted Glass Bottom Navigation Bar
        PanelBottomNavBar(
            selectedTab = PanelTab.MORE,
            onTabSelected = onTabSelected,
            hazeState = hazeState,
            modifier = Modifier.align(Alignment.BottomCenter)
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

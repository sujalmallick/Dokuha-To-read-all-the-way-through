package ui.screens.more

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.mock.FakePanelRepository
import data.mock.MockStorageInfo
import data.mock.PanelRepository
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.PanelTab
import ui.screens.browse.BrowseScreen
import ui.screens.details.TitleDetailsScreen
import ui.screens.downloads.DownloadsScreenContent
import ui.screens.library.LibraryScreen
import ui.screens.updates.UpdatesScreen
import ui.theme.PanelTheme

class MoreScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()

        val storageInfo by repository.getStorageInfo().collectAsState(initial = MockStorageInfo())
        val downloads by repository.getDownloads().collectAsState(initial = emptyList())

        DownloadsScreenContent(
            storageInfo = storageInfo,
            initialDownloads = downloads,
            onOpenManga = { manga -> navigator.push(TitleDetailsScreen(manga.url)) },
            onTabSelected = { tab ->
                when (tab) {
                    PanelTab.LIBRARY -> navigator.push(LibraryScreen())
                    PanelTab.UPDATES -> navigator.push(UpdatesScreen())
                    PanelTab.BROWSE -> navigator.push(BrowseScreen())
                    PanelTab.MORE -> {} // Current
                }
            }
        )
    }
}

@Preview
@Composable
fun MoreScreenPreview() {
    PanelTheme {
        DownloadsScreenContent(
            storageInfo = MockStorageInfo(),
            initialDownloads = emptyList(),
            onOpenManga = {},
            onTabSelected = {}
        )
    }
}

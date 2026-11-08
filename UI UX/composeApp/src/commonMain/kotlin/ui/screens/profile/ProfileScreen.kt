package ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.profile.DayReadingActivity
import data.profile.FakeProfileRepository
import data.profile.LibrarySummary
import data.profile.ProfileRepository
import data.profile.ReadingStats
import data.profile.TrackerAccount
import data.profile.UserProfile
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.ActivityBarChart
import ui.components.EditProfileSheet
import ui.components.GlassCollapsingTopBar
import ui.components.ProfileHeader
import ui.components.ProfileStatsRow
import ui.components.SectionHeader
import ui.components.SettingsGroup
import ui.components.SettingsRow
import ui.components.SettingsSeparator
import ui.components.TrackerRow
import ui.theme.PanelBackground
import ui.theme.PanelTheme

class ProfileScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: ProfileRepository = koinInject()

        val profile by repository.profile.collectAsState()
        val stats by repository.stats.collectAsState()
        val weeklyActivity by repository.weeklyActivity.collectAsState()
        val trackers by repository.trackers.collectAsState()
        val librarySummary by repository.librarySummary.collectAsState()

        ProfileScreenContent(
            profile = profile,
            stats = stats,
            weeklyActivity = weeklyActivity,
            trackers = trackers,
            librarySummary = librarySummary,
            onBack = { navigator.pop() },
            onUpdateProfile = { name, colorHex ->
                repository.updateProfile(name, colorHex)
            },
            onToggleTracker = { service ->
                repository.toggleTrackerConnection(service)
            }
        )
    }
}

@Composable
fun ProfileScreenContent(
    profile: UserProfile,
    stats: ReadingStats,
    weeklyActivity: List<DayReadingActivity>,
    trackers: List<TrackerAccount>,
    librarySummary: LibrarySummary,
    onBack: () -> Unit,
    onUpdateProfile: (String, Long) -> Unit,
    onToggleTracker: (data.profile.TrackerService) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()
    var isEditSheetVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PanelBackground)
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 64.dp, bottom = 48.dp)
        ) {
            // Profile Header (Avatar + Name + Tenure + Glass Edit Button)
            item {
                ProfileHeader(
                    profile = profile,
                    onEditClick = { isEditSheetVisible = true }
                )
            }

            // Numeric Stats Row: Chapters read / Titles / Hours
            item {
                ProfileStatsRow(stats = stats)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Weekly Reading Activity Canvas Flat Bar Chart
            item {
                ActivityBarChart(weeklyActivity = weeklyActivity)
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Library Summary Group (Reading / Completed / Plan to read as Value rows)
            item {
                SectionHeader(text = "Library Overview")
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SettingsGroup {
                        SettingsRow.Value(
                            title = "Currently Reading",
                            value = "${librarySummary.readingCount} titles"
                        )
                        SettingsSeparator()
                        SettingsRow.Value(
                            title = "Completed",
                            value = "${librarySummary.completedCount} titles"
                        )
                        SettingsSeparator()
                        SettingsRow.Value(
                            title = "Plan to Read",
                            value = "${librarySummary.planToReadCount} titles"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Tracking Services Section (AniList, MyAnimeList, Kitsu)
            item {
                SectionHeader(text = "Trackers")
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SettingsGroup {
                        trackers.forEachIndexed { index, tracker ->
                            TrackerRow(
                                tracker = tracker,
                                onToggle = { onToggleTracker(tracker.service) }
                            )
                            if (index < trackers.size - 1) {
                                SettingsSeparator()
                            }
                        }
                    }
                }
            }
        }

        // Collapsing Lens Glass Top Bar with Circular Back Button
        val scrollOffset = scrollState.firstVisibleItemScrollOffset.toFloat() +
            (scrollState.firstVisibleItemIndex * 100f)

        GlassCollapsingTopBar(
            title = profile.name,
            onBack = onBack,
            scrollOffset = scrollOffset
        )

        // Glass Edit Profile Sheet
        if (isEditSheetVisible) {
            EditProfileSheet(
                profile = profile,
                onSave = onUpdateProfile,
                onDismiss = { isEditSheetVisible = false }
            )
        }
    }
}

@Preview
@Composable
fun ProfileScreenPreview() {
    PanelTheme {
        ProfileScreenContent(
            profile = UserProfile(),
            stats = ReadingStats(),
            weeklyActivity = listOf(
                DayReadingActivity("M", 45),
                DayReadingActivity("T", 90),
                DayReadingActivity("W", 30),
                DayReadingActivity("T", 120),
                DayReadingActivity("F", 60),
                DayReadingActivity("S", 150),
                DayReadingActivity("S", 85)
            ),
            trackers = listOf(
                TrackerAccount(data.profile.TrackerService.ANILIST, isConnected = true, username = "sujal_reads"),
                TrackerAccount(data.profile.TrackerService.MY_ANIME_LIST, isConnected = false)
            ),
            librarySummary = LibrarySummary(),
            onBack = {},
            onUpdateProfile = { _, _ -> },
            onToggleTracker = {}
        )
    }
}

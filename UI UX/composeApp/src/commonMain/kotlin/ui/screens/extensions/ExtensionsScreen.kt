package ui.screens.extensions

import core.logging.AppLogger
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.extensions.ExtensionManager
import core.extensions.js.ExtensionCatalogItem
import core.extensions.js.ExtensionType
import core.extensions.js.JsExtensionManifest
import core.extensions.model.ExtensionEcosystem
import data.extension.DefaultExtensionRepository
import data.extension.ExtensionItem
import data.extension.ExtensionRepository
import data.extension.ExtensionSourceInfo
import data.extension.ExtensionStatus
import data.extension.ExtensionTrustLevel
import data.mock.MockExtension
import data.mock.PanelRepository
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.GlassSegmentedControl
import ui.components.PanelBottomNavBar
import ui.components.PanelTab
import ui.glass.glassPress
import ui.glass.glassSource
import ui.glass.rememberPanelGlassState
import ui.screens.browse.BrowseScreen
import ui.screens.browse.SourceDetailScreen
import ui.screens.library.LibraryScreen
import ui.screens.maintab.MainTabScreen
import ui.screens.more.MoreScreen
import ui.screens.updates.UpdatesScreen
import ui.theme.CapsuleShape
import ui.theme.ElectricBlue
import ui.theme.ElectricBlueLight
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
        val extensionManager: ExtensionManager = koinInject()
        val coroutineScope = rememberCoroutineScope()

        val installedExts by extensionManager.installedExtensions.collectAsState(initial = emptyList())
        val availableCatalog by extensionManager.availableExtensions.collectAsState(initial = emptyList())
        val extensionVariants by extensionManager.extensionVariants.collectAsState(initial = emptyMap())
        val initError by extensionManager.initError.collectAsState()
        val fallbackMockExtensions by repository.extensions.collectAsState(initial = emptyList())

        // Map live ExtensionManager data to upstream ExtensionItem models
        val effectiveExtensions = remember(installedExts, availableCatalog, fallbackMockExtensions) {
            val list = mutableListOf<ExtensionItem>()
            val installedIds = installedExts.map { it.id }.toSet()
            val installedKeys = installedExts.map { "${it.name.lowercase().trim()}::${it.lang.lowercase().trim()}" }.toSet()

            // 1. Installed extensions
            for (inst in installedExts) {
                // Check if an update is available in catalog
                val catalogMatch = availableCatalog.firstOrNull { it.id == inst.id }
                val hasUpdate = catalogMatch != null && catalogMatch.version != inst.version

                list.add(
                    ExtensionItem(
                        id = inst.id,
                        name = inst.name,
                        pkgName = "dokuha.extension.${inst.id.replace('-', '.')}",
                        versionName = inst.version,
                        versionCode = 1L,
                        lang = inst.lang.uppercase(),
                        isNsfw = inst.isNsfw,
                        trustLevel = trustLevelFor(inst.ecosystem, inst.baseUrl),
                        repoUrl = inst.baseUrl,
                        status = if (hasUpdate) ExtensionStatus.UPDATE_AVAILABLE else ExtensionStatus.INSTALLED,
                        updateVersion = if (hasUpdate) catalogMatch?.version else null,
                        iconColorHex = brandColorFor(inst.name),
                        description = "Installed extension for reading ${if (inst.type == ExtensionType.NOVEL) "web novels" else "manga & comics"} from ${inst.name}.",
                        sources = listOf(
                            ExtensionSourceInfo(
                                id = inst.id,
                                name = inst.name,
                                baseUrl = inst.baseUrl,
                                languages = listOf(inst.lang.uppercase()),
                                contentType = if (inst.type == ExtensionType.NOVEL) "Novel" else "Manga"
                            )
                        ),
                        networkDomains = listOf(inst.baseUrl.substringAfter("://").substringBefore('/'))
                    )
                )
            }

            // 2. Available extensions (deduplicated against installed and other catalog variants)
            val seenKeys = mutableSetOf<String>()
            for (avail in availableCatalog) {
                val key = "${avail.name.lowercase().trim()}::${avail.lang.lowercase().trim()}"
                if (installedIds.contains(avail.id) || installedKeys.contains(key) || seenKeys.contains(key)) {
                    continue
                }
                seenKeys.add(key)
                list.add(
                    ExtensionItem(
                        id = avail.id,
                        name = avail.name,
                        pkgName = avail.pkg.ifBlank { "dokuha.extension.${avail.id.replace('-', '.')}" },
                        versionName = avail.version,
                        versionCode = 1L,
                        lang = avail.lang.uppercase(),
                        isNsfw = avail.isNsfw,
                        trustLevel = trustLevelFor(avail.ecosystem, avail.repoBaseUrl),
                        repoUrl = avail.repoBaseUrl,
                        status = ExtensionStatus.AVAILABLE,
                        iconColorHex = brandColorFor(avail.name),
                        description = "Community extension for ${if (avail.type == ExtensionType.NOVEL) "web novels" else "manga"} from ${avail.name}.",
                        sources = listOf(
                            ExtensionSourceInfo(
                                id = avail.id,
                                name = avail.name,
                                baseUrl = avail.baseUrl,
                                languages = listOf(avail.lang.uppercase()),
                                contentType = if (avail.type == ExtensionType.NOVEL) "Novel" else "Manga"
                            )
                        ),
                        networkDomains = listOf(avail.baseUrl.substringAfter("://").substringBefore('/'))
                    )
                )
            }

            // Also include sideloaded/custom extensions from repository
            val extraRepoExtensions = fallbackMockExtensions.filterNot { extra ->
                list.any { it.id == extra.id }
            }
            list + extraRepoExtensions
        }

        // Pending install that needs repo selection (set when multiple repos provide the extension)
        var pendingInstallItem by remember { mutableStateOf<ExtensionItem?>(null) }

        val pendingVariants = remember(pendingInstallItem, extensionVariants) {
            val item = pendingInstallItem ?: return@remember emptyList()
            val key = "${item.name.lowercase().trim()}::${item.lang.lowercase().trim()}"
            extensionVariants[key] ?: emptyList()
        }

        val pendingItem = pendingInstallItem
        if (pendingItem != null && pendingVariants.isNotEmpty()) {
            RepoPickerDialog(
                extensionName = pendingItem.name,
                variants = pendingVariants,
                onSelect = { chosen ->
                    pendingInstallItem = null
                    coroutineScope.launch {
                        val success = extensionManager.installExtension(chosen, chosen.repoBaseUrl)
                        AppLogger.d("ExtensionsScreen", "Install from repo picker result: $success")
                    }
                },
                onDismiss = { pendingInstallItem = null }
            )
        }

        ExtensionsScreenContent(
            extensions = effectiveExtensions,
            extensionRepository = repository,
            initError = initError,
            onInstall = { ext ->
                val key = "${ext.name.lowercase().trim()}::${ext.lang.lowercase().trim()}"
                val variants = extensionVariants[key] ?: emptyList()
                if (variants.size > 1) {
                    pendingInstallItem = ext
                } else {
                    coroutineScope.launch {
                        val catalogItem = availableCatalog.firstOrNull { it.id == ext.id }
                        if (catalogItem != null) {
                            val repoBase = catalogItem.repoBaseUrl.ifBlank {
                                extensionManager.repoUrls.value.firstOrNull() ?: ""
                            }
                            extensionManager.installExtension(catalogItem, repoBase)
                        } else {
                            repository.installExtension(ext.id)
                        }
                    }
                }
            },
            onUpdate = { ext ->
                coroutineScope.launch {
                    val catalogItem = availableCatalog.firstOrNull { it.id == ext.id }
                    if (catalogItem != null) {
                        val repoBase = catalogItem.repoBaseUrl.ifBlank {
                            extensionManager.repoUrls.value.firstOrNull() ?: ""
                        }
                        extensionManager.installExtension(catalogItem, repoBase)
                    } else {
                        repository.updateExtension(ext.id)
                    }
                }
            },
            onUninstall = { ext ->
                coroutineScope.launch {
                    extensionManager.uninstallExtension(ext.id)
                    repository.uninstallExtension(ext.id)
                }
            },
            onClearCache = { ext ->
                repository.clearExtensionCache(ext.id)
            },
            onBrowse = { ext ->
                navigator.push(SourceDetailScreen(ext.id, ext.name))
            },
            onAddRepo = { url, name ->
                val result = extensionManager.addRepository(url)
                repository.addRepository(url, name)
                coroutineScope.launch {
                    extensionManager.refreshCatalog()
                }
                result.isValid
            },
            onRemoveRepo = { repoId ->
                repository.removeRepository(repoId)
            },
            onUpdateAll = {
                repository.updateAllExtensions()
            },
            onRefresh = {
                coroutineScope.launch {
                    extensionManager.refreshCatalog()
                }
            },
            onLongClick = { ext ->
                val key = "${ext.name.lowercase().trim()}::${ext.lang.lowercase().trim()}"
                val variants = extensionVariants[key] ?: emptyList()
                if (variants.isNotEmpty()) {
                    pendingInstallItem = ext
                }
            },
            onTabSelected = { tab ->
                navigator.replaceAll(MainTabScreen(tab))
            }
        )
    }
}

@Composable
fun ExtensionsScreenContent(
    extensions: List<ExtensionItem> = emptyList(),
    extensionRepository: ExtensionRepository? = null,
    initError: String? = null,
    onTabSelected: (PanelTab) -> Unit = {},
    onInstall: (ExtensionItem) -> Unit = {},
    onUpdate: (ExtensionItem) -> Unit = {},
    onUninstall: (ExtensionItem) -> Unit = {},
    onClearCache: (ExtensionItem) -> Unit = {},
    onBrowse: (ExtensionItem) -> Unit = {},
    onAddRepo: (url: String, name: String?) -> Boolean = { _, _ -> true },
    onRemoveRepo: (repoId: String) -> Boolean = { true },
    onUpdateAll: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onLongClick: (ExtensionItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val repository = remember(extensionRepository) {
        extensionRepository ?: DefaultExtensionRepository()
    }
    val repos by repository.repositories.collectAsState()

    val glassState = rememberPanelGlassState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf("ALL") }
    val hasInstalled = extensions.any { it.status == ExtensionStatus.INSTALLED || it.status == ExtensionStatus.UPDATE_AVAILABLE }
    var selectedStatusTab by remember { mutableStateOf(if (hasInstalled) "Installed" else "Available") }

    var selectedExtensionForSheet by remember { mutableStateOf<ExtensionItem?>(null) }
    var isAddRepoVisible by remember { mutableStateOf(false) }
    var isSideloadVisible by remember { mutableStateOf(false) }

    // Derive languages from actual extensions list + standard defaults
    val languages = remember(extensions) {
        val extra = extensions.map { it.lang }.filter { it.isNotBlank() && it != "ALL" }.distinct().sorted()
        (listOf("ALL", "EN", "JA", "KO", "ES", "FR") + extra).distinct()
    }

    // Tab counts
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
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.25f),
                    radius = (size.width * 0.65f).coerceAtLeast(1f)
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFA855F7).copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.55f),
                    radius = (size.width * 0.60f).coerceAtLeast(1f)
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
                    if (!initError.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.16f))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Extension restore warning: $initError",
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp
                            )
                        }
                    }
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

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Refresh Button
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                                    .clickable { onRefresh() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Catalog",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Sideload Pill Button
                            Box(
                                modifier = Modifier
                                    .clip(CapsuleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.16f), CapsuleShape)
                                    .clickable { isSideloadVisible = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sideload",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }

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
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Glass Search Field
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
                                    onValueChange = { searchQuery = it },
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
                                        .clickable { searchQuery = "" }
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
                                    .clickable { selectedLang = lang }
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
                    if (repos.isEmpty() && extensions.isEmpty() && searchQuery.isEmpty()) {
                        // Zero-repo clean empty state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp, bottom = 48.dp, start = 16.dp, end = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 24.dp, vertical = 32.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Repositories Added",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Add an extension repository or sideload an extension script to discover manga and comic sources.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF38BDF8))
                                            .clickable { isAddRepoVisible = true }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Add Repository",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF090D15)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                            .clickable { isSideloadVisible = true }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Sideload",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    } else {
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
                }
            } else {
                if (selectedStatusTab == "Updates" && updatesCount > 0) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .clickable { onUpdateAll() }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Update All ($updatesCount)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "Update",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }

                items(filteredExtensions, key = { it.id }) { ext ->
                    ExtensionCardItem(
                        extension = ext,
                        onCardClick = { selectedExtensionForSheet = ext },
                        onBrowseClick = { onBrowse(ext) },
                        onInstallClick = { onInstall(ext) },
                        onUpdateClick = { onUpdate(ext) },
                        onSettingsClick = { selectedExtensionForSheet = ext },
                        onLongClick = if (ext.status == ExtensionStatus.AVAILABLE) {
                            { onLongClick(ext) }
                        } else null
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
            onBrowse = { ext ->
                selectedExtensionForSheet = null
                onBrowse(ext)
            },
            onInstall = { ext ->
                onInstall(ext)
                selectedExtensionForSheet = null
            },
            onUpdate = { ext ->
                onUpdate(ext)
                selectedExtensionForSheet = null
            },
            onUninstall = { ext ->
                onUninstall(ext)
                selectedExtensionForSheet = null
            },
            onClearCache = { ext ->
                onClearCache(ext)
            }
        )

        // LAYER 4: Add Repository Dialog
        AddRepoDialog(
            isVisible = isAddRepoVisible,
            onDismiss = { isAddRepoVisible = false },
            onAddRepo = { url, name ->
                onAddRepo(url, name)
            },
            repositories = repos,
            onRemoveRepo = { repoId ->
                onRemoveRepo(repoId)
            }
        )

        // LAYER 5: Sideload Extension Dialog
        SideloadExtensionDialog(
            isVisible = isSideloadVisible,
            onDismiss = { isSideloadVisible = false },
            repository = repository,
            onExtensionInstalled = {
                onRefresh()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExtensionCardItem(
    extension: ExtensionItem,
    onCardClick: () -> Unit,
    onInstallClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBrowseClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
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
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onCardClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onCardClick
                    )
                }
            )
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val browseInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .clip(CapsuleShape)
                                .glassPress(browseInteraction)
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.25f), CapsuleShape)
                                .clickable(
                                    interactionSource = browseInteraction,
                                    indication = null,
                                    onClick = onBrowseClick
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Browse",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onSettingsClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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

/**
 * Liquid glass dialog shown when multiple repositories provide the same extension.
 * Lists each repo variant with its version and repository URL.
 */
@Composable
private fun RepoPickerDialog(
    extensionName: String,
    variants: List<ExtensionCatalogItem>,
    onSelect: (ExtensionCatalogItem) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF131720))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header
                Text(
                    text = "Choose Repository",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Multiple repositories provide \"$extensionName\". Pick the variant to install:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // One row per repo variant
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    variants.forEach { variant ->
                        RepoVariantRow(variant = variant, onClick = { onSelect(variant) })
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // Cancel Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun RepoVariantRow(
    variant: ExtensionCatalogItem,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val displayRepo = variant.repoBaseUrl
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .trimEnd('/')
                Text(
                    text = displayRepo.ifBlank { "Unknown Repository" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "v${variant.version} • ${variant.lang.uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(CapsuleShape)
                    .background(Color.White)
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
    }
}

private fun brandColorFor(name: String): Long {
    val lower = name.lowercase()
    return when {
        lower.contains("mangadex") -> 0xFFFF6740
        lower.contains("comick") -> 0xFF60A5FA
        lower.contains("asura") -> 0xFFA855F7
        lower.contains("webnovel") -> 0xFFEC4899
        lower.contains("flame") -> 0xFFF97316
        lower.contains("mangayomi") || lower.contains("kodjodevf") -> 0xFF00DC64
        lower.contains("lnreader") -> 0xFF8B5CF6
        lower.contains("paperback") -> 0xFFF59E0B
        else -> {
            val palette = listOf(0xFF38BDF8, 0xFFA855F7, 0xFFEC4899, 0xFF10B981, 0xFFF59E0B, 0xFF6366F1, 0xFF06B6D4)
            palette[kotlin.math.abs(name.hashCode()) % palette.size]
        }
    }
}

private fun trustLevelFor(ecosystem: ExtensionEcosystem, repoUrl: String): ExtensionTrustLevel {
    val lower = repoUrl.lowercase()
    return when {
        lower.contains("dokuha") || lower.contains("tachiyomi") || ecosystem == ExtensionEcosystem.CUSTOM -> ExtensionTrustLevel.OFFICIAL
        lower.contains("keiyoushi") || lower.contains("mangayomi") || lower.contains("lnreader") || lower.contains("paperback") -> ExtensionTrustLevel.COMMUNITY
        else -> ExtensionTrustLevel.COMMUNITY
    }
}

@Preview
@Composable
fun ExtensionsScreenPreview() {
    PanelTheme {
        ExtensionsScreenContent()
    }
}

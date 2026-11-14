package ui.screens.extensions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.extensions.ExtensionManager
import data.mock.MockExtension
import data.mock.PanelRepository
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import ui.components.ExtensionItemCard
import ui.components.GlassConfirmDialog
import ui.components.GlassSegmentedControl
import ui.components.PanelBottomNavBar
import ui.components.PanelTab
import ui.glass.glassSource
import ui.glass.rememberPanelGlassState
import ui.screens.browse.BrowseScreen
import ui.screens.library.LibraryScreen
import ui.screens.more.MoreScreen
import ui.screens.updates.UpdatesScreen
import ui.theme.*

class ExtensionsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository: PanelRepository = koinInject()
        val extensionManager: ExtensionManager = koinInject()
        val coroutineScope = rememberCoroutineScope()

        val installedExts by extensionManager.installedExtensions.collectAsState(initial = emptyList())
        val availableCatalog by extensionManager.availableExtensions.collectAsState(initial = emptyList())

        val effectiveExtensions = remember(installedExts, availableCatalog) {
            val installedKeys = installedExts.map { "${it.name.lowercase().trim()}::${it.lang.lowercase().trim()}" }.toSet()
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
            val seenAvailableKeys = mutableSetOf<String>()
            for (avail in availableCatalog) {
                val key = "${avail.name.lowercase().trim()}::${avail.lang.lowercase().trim()}"
                if (!installedIds.contains(avail.id) && !installedKeys.contains(key) && !seenAvailableKeys.contains(key)) {
                    seenAvailableKeys.add(key)
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
        }

        ExtensionsScreenContent(
            initialExtensions = effectiveExtensions,
            onActionClick = { ext ->
                coroutineScope.launch {
                    println("[ExtensionsScreen] onActionClick called for ${ext.name} (id: ${ext.id}, isInstalled: ${ext.isInstalled})")
                    if (ext.isInstalled) {
                        extensionManager.uninstallExtension(ext.id)
                    } else {
                        val catalogItem = availableCatalog.firstOrNull { it.id == ext.id }
                        println("[ExtensionsScreen] catalogItem found: ${catalogItem?.name} (pkg: ${catalogItem?.pkg})")
                        if (catalogItem != null) {
                            val repoBase = extensionManager.repoUrls.value.firstOrNull() ?: ""
                            val success = extensionManager.installExtension(catalogItem, repoBase)
                            println("[ExtensionsScreen] install result: $success")
                        }
                    }
                }
            },
            onDeleteClick = { ext ->
                coroutineScope.launch {
                    println("[ExtensionsScreen] onDeleteClick called for ${ext.name} (id: ${ext.id})")
                    extensionManager.uninstallExtension(ext.id)
                }
            },
            onAddRepository = { url ->
                val result = extensionManager.addRepository(url)
                result.isValid
            },
            onRefresh = {
                coroutineScope.launch {
                    extensionManager.refreshCatalog()
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
    onDeleteClick: (MockExtension) -> Unit = {},
    onAddRepository: (String) -> Boolean = { true },
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val glassState = rememberPanelGlassState()
    val hasInstalled = initialExtensions.any { it.isInstalled }
    var selectedFilter by remember { mutableStateOf(if (hasInstalled) "Installed" else "Available") }
    val filters = listOf("Installed", "Available", "Updates")

    var showAddDialog by remember { mutableStateOf(false) }
    var extensionToUninstall by remember { mutableStateOf<MockExtension?>(null) }
    var newRepoUrl by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf("ALL") }

    // State list for interactive updates
    val extensionsList = remember(initialExtensions) {
        mutableStateListOf<MockExtension>().apply {
            addAll(initialExtensions)
        }
    }

    val filteredExtensions = extensionsList.filter { ext ->
        val matchesTab = when (selectedFilter) {
            "Installed" -> ext.isInstalled
            "Available" -> !ext.isInstalled
            "Updates" -> ext.hasUpdate
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() || ext.name.contains(searchQuery, ignoreCase = true)
        val matchesLang = selectedLang == "ALL" || ext.lang.equals(selectedLang, ignoreCase = true)
        matchesTab && matchesSearch && matchesLang
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

        // LAYER 1: Scrollable Content Layer
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
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = onRefresh) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Catalog",
                                    tint = TextSecondary
                                )
                            }
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Repository",
                                    tint = ElectricBlue
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Unified Glass Segmented Control with sliding glass lens indicator
                    GlassSegmentedControl(
                        items = filters,
                        selectedIndex = filters.indexOf(selectedFilter),
                        onItemSelected = { selectedFilter = filters[it] },
                        labelProvider = { it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Liquid Glass Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search extensions...", color = TextSecondary, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedContainerColor = Color.White.copy(alpha = 0.04f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Language Quick Filter Chips
                    val availableLangs = remember(extensionsList) {
                        val langs = extensionsList.map { it.lang }.filter { it.isNotBlank() && it != "ALL" }.distinct().sorted()
                        listOf("ALL") + langs
                    }
                    if (availableLangs.size > 2) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableLangs) { lang ->
                                val isSelected = selectedLang.equals(lang, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ElectricBlue.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) ElectricBlue.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedLang = lang }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = lang,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) ElectricBlue else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Extensions Items List
            if (filteredExtensions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val (title, subtitle) = when (selectedFilter) {
                                "Installed" -> "No Extensions Installed" to "Browse the Available tab to install sources."
                                "Available" -> "No Extensions Available" to "Add a repository URL to discover and install community sources."
                                else -> "All Up to Date" to "All installed extensions are at their latest version."
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )

                            if (selectedFilter == "Available") {
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { showAddDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Repository", color = Color.White)
                                }
                            }
                        }
                    }
                }
            } else {
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
                        lang = ext.lang,
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
                        },
                        onDeleteClick = if (ext.isInstalled) {
                            {
                                extensionToUninstall = ext
                            }
                        } else null
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Add Repository Dialog
        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false; addError = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF161A22))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Add Extension Repository",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "Enter a repository URL or choose a popular preset below:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        // Quick presets
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickPresetChip("Mangayomi (Manga & Novels)") {
                                newRepoUrl = "https://kodjodevf.github.io/mangayomi-extensions"
                            }
                            QuickPresetChip("LNReader (370+ Web Novels)") {
                                newRepoUrl = "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist"
                            }
                            QuickPresetChip("Paperback (Default Comics)") {
                                newRepoUrl = "https://paperback-ios.github.io/extensions-default/0.8"
                            }
                        }

                        OutlinedTextField(
                            value = newRepoUrl,
                            onValueChange = { newRepoUrl = it; addError = null },
                            label = { Text("Repository HTTPS URL") },
                            isError = addError != null,
                            supportingText = addError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showAddDialog = false; addError = null }) {
                                Text("Cancel", color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val success = onAddRepository(newRepoUrl.trim())
                                    if (success) {
                                        showAddDialog = false
                                        newRepoUrl = ""
                                        addError = null
                                        selectedFilter = "Available"
                                    } else {
                                        addError = "Invalid or insecure repository URL"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                enabled = newRepoUrl.isNotBlank()
                            ) {
                                Text("Add Repository")
                            }
                        }
                    }
                }
            }
        }

        // Uninstall Confirmation Dialog
        if (extensionToUninstall != null) {
            val ext = extensionToUninstall!!
            GlassConfirmDialog(
                title = "Uninstall Extension?",
                message = "Are you sure you want to uninstall '${ext.name}'? Its sources and catalog items will be removed.",
                confirmText = "Uninstall",
                isDestructive = true,
                onConfirm = {
                    val index = extensionsList.indexOfFirst { it.id == ext.id }
                    if (index != -1) {
                        val current = extensionsList[index]
                        extensionsList[index] = current.copy(
                            isInstalled = false,
                            hasUpdate = false
                        )
                    }
                    onDeleteClick(ext)
                    extensionToUninstall = null
                },
                onDismiss = { extensionToUninstall = null }
            )
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

@Composable
private fun QuickPresetChip(title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF222734))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "+ $title",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = ElectricBlue
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

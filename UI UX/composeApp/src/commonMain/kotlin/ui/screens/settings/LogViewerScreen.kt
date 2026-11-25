package ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import core.logging.AppLogger
import core.logging.LogLevel
import ui.components.GlassCollapsingTopBar
import ui.theme.BadgeRed
import ui.theme.PanelBackground
import ui.theme.TextMuted
import ui.theme.TextPrimary
import ui.theme.TextSecondary

class LogViewerScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val logs by AppLogger.logs.collectAsState()
        var searchQuery by remember { mutableStateOf("") }
        var selectedFilter by remember { mutableStateOf<LogLevel?>(null) }
        val listState = rememberLazyListState()

        val filteredLogs = remember(logs, searchQuery, selectedFilter) {
            logs.filter { entry ->
                val matchesSearch = if (searchQuery.isBlank()) true else {
                    entry.tag.contains(searchQuery, ignoreCase = true) ||
                        entry.message.contains(searchQuery, ignoreCase = true)
                }
                val matchesLevel = when (selectedFilter) {
                    null -> true
                    LogLevel.ERROR -> entry.level == LogLevel.ERROR
                    LogLevel.WARN -> entry.level == LogLevel.WARN || entry.level == LogLevel.ERROR
                    else -> entry.level == selectedFilter
                }
                matchesSearch && matchesLevel
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PanelBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp)
            ) {
                // Header / Search Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Filter logs by tag or keyword...", fontSize = 13.sp, color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFF2D3139),
                            focusedContainerColor = Color(0xFF14171E),
                            unfocusedContainerColor = Color(0xFF14171E)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Chips and Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedFilter == null,
                                onClick = { selectedFilter = null },
                                label = { Text("All", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF6366F1).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(0xFF818CF8)
                                )
                            )
                            FilterChip(
                                selected = selectedFilter == LogLevel.WARN,
                                onClick = { selectedFilter = if (selectedFilter == LogLevel.WARN) null else LogLevel.WARN },
                                label = { Text("Warn+", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(0xFFFBBF24)
                                )
                            )
                            FilterChip(
                                selected = selectedFilter == LogLevel.ERROR,
                                onClick = { selectedFilter = if (selectedFilter == LogLevel.ERROR) null else LogLevel.ERROR },
                                label = { Text("Errors", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BadgeRed.copy(alpha = 0.2f),
                                    selectedLabelColor = Color(0xFFF87171)
                                )
                            )
                        }

                        Row {
                            IconButton(
                                onClick = { AppLogger.shareLogs() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Export Logs", tint = Color(0xFF818CF8), modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { AppLogger.clear() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Log List
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (logs.isEmpty()) "No logs recorded yet" else "No matching logs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = filteredLogs,
                            key = { entry -> "${entry.timestamp}_${entry.level}_${entry.tag}_${entry.message.hashCode()}" }
                        ) { entry ->
                            val levelColor = when (entry.level) {
                                LogLevel.ERROR -> Color(0xFFEF4444)
                                LogLevel.WARN -> Color(0xFFF59E0B)
                                LogLevel.INFO -> Color(0xFF10B981)
                                LogLevel.DEBUG -> Color(0xFF6B7280)
                                LogLevel.VERBOSE -> Color(0xFF4B5563)
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF11141B), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "[${entry.level.name}] ${entry.tag}",
                                        color = levelColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = entry.message,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (entry.throwable != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = entry.throwable,
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            GlassCollapsingTopBar(
                title = "Diagnostic Logs",
                onBack = { navigator.pop() },
                scrollOffset = 0f
            )
        }
    }
}

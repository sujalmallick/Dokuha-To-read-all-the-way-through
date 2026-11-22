package ui.screens.extensions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.extensions.ExtensionRepoValidator
import data.extension.ExtensionRepo
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.TextMuted
import ui.theme.TextPrimary
import ui.theme.TextSecondary
import ui.theme.TextTertiary

@Composable
fun AddRepoDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onAddRepo: (url: String, name: String?) -> Unit,
    repositories: List<ExtensionRepo> = emptyList(),
    onRemoveRepo: (repoId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    var rawUrl by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }

    val validation = remember(rawUrl) {
        if (rawUrl.isBlank()) null
        else ExtensionRepoValidator.validateAndNormalize(rawUrl)
    }

    val isValid = validation?.isValid == true

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            val dialogShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume inner click
                    )
                    .clip(dialogShape)
                    .background(Color(0xF5111520))
                    .border(1.dp, GlassBorderStrong, dialogShape)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Repository Manager",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Manage active extension sources or add custom repositories. Insecure HTTP and private addresses are strictly blocked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    // Active Repositories List
                    if (repositories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Active Repositories (${repositories.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextTertiary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            repositories.forEach { repo ->
                                ActiveRepoRow(
                                    repo = repo,
                                    onDelete = { onRemoveRepo(repo.id) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Popular Community Presets
                    Text(
                        text = "Quick Presets",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RepoPresetChip("Dokuha Official", "https://raw.githubusercontent.com/RitwikGupta-0501/dokuha-extensions/main") {
                            rawUrl = "https://raw.githubusercontent.com/RitwikGupta-0501/dokuha-extensions/main"
                            customName = "Dokuha Official"
                        }
                        RepoPresetChip("Keiyoushi Community", "https://raw.githubusercontent.com/keiyoushi/extensions/repo") {
                            rawUrl = "https://raw.githubusercontent.com/keiyoushi/extensions/repo"
                            customName = "Keiyoushi Community"
                        }
                        RepoPresetChip("Mangayomi (Manga & Novels)", "https://kodjodevf.github.io/mangayomi-extensions") {
                            rawUrl = "https://kodjodevf.github.io/mangayomi-extensions"
                            customName = "Mangayomi"
                        }
                        RepoPresetChip("LNReader (370+ Web Novels)", "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist") {
                            rawUrl = "https://raw.githubusercontent.com/LNReader/lnreader-plugins/plugins/v3.0.0/.dist"
                            customName = "LNReader"
                        }
                        RepoPresetChip("Paperback (Default Comics)", "https://paperback-ios.github.io/extensions-default/0.8") {
                            rawUrl = "https://paperback-ios.github.io/extensions-default/0.8"
                            customName = "Paperback"
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // URL Input Field
                    Text(
                        text = "Repository URL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(
                                1.dp,
                                when {
                                    validation == null -> GlassBorder
                                    isValid -> Color(0xFF00DC64).copy(alpha = 0.6f)
                                    else -> Color(0xFFEF4444).copy(alpha = 0.6f)
                                },
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (rawUrl.isEmpty()) {
                            Text(
                                text = "https://example.com/extensions/repo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                        BasicTextField(
                            value = rawUrl,
                            onValueChange = { rawUrl = it },
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Validation Feedback
                    if (validation != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isValid) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF00DC64),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Fingerprint: ${validation.fingerprint?.take(24)}...",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF00DC64),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = validation.errorMessage ?: "Invalid URL",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFEF4444),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Optional Repo Name Input
                    Text(
                        text = "Repository Name (Optional)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (customName.isEmpty()) {
                            Text(
                                text = "e.g. Keiyoushi Community",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                        BasicTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Cancel
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { onDismiss() }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Add
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isValid) Color.White else Color.White.copy(alpha = 0.20f))
                                .clickable(enabled = isValid) {
                                    if (isValid) {
                                        onAddRepo(rawUrl, customName.takeIf { it.isNotBlank() })
                                        onDismiss()
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Add Repo",
                                color = if (isValid) Color(0xFF090D15) else Color.White.copy(alpha = 0.40f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveRepoRow(
    repo: ExtensionRepo,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = repo.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (repo.isOfficial) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF00DC64).copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "OFFICIAL",
                                color = Color(0xFF00DC64),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = repo.url,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!repo.isOfficial) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Repo",
                        tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RepoPresetChip(
    title: String,
    url: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+ $title",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF38BDF8)
            )
            Text(
                text = url.substringAfter("://").substringBefore('/'),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = TextSecondary.copy(alpha = 0.6f)
            )
        }
    }
}

package ui.screens.extensions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.extension.ExtensionItem
import data.extension.ExtensionStatus
import data.extension.ExtensionTrustLevel
import ui.components.PanelCircleIconButton
import ui.components.glass.GlassStyle
import ui.components.glass.GlassSurface
import ui.glass.GlassSurfaceContext
import ui.theme.BottomSheetShape
import ui.theme.CapsuleShape
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.TextMuted
import ui.theme.TextPrimary
import ui.theme.TextSecondary
import ui.theme.TextTertiary

@Composable
fun ExtensionDetailSheet(
    extension: ExtensionItem?,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onInstall: (ExtensionItem) -> Unit,
    onUpdate: (ExtensionItem) -> Unit,
    onUninstall: (ExtensionItem) -> Unit,
    onClearCache: (ExtensionItem) -> Unit,
    onBrowse: ((ExtensionItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (extension == null) return

    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val brandColor = Color(extension.iconColorHex)

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.60f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {} // Consume inner click
                        ),
                    shape = BottomSheetShape,
                    style = GlassStyle.SHEET,
                    context = GlassSurfaceContext.STANDARD
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 22.dp, vertical = 14.dp)
                    ) {
                        // Minimal Sheet Drag Handle
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 36.dp, height = 4.dp)
                                .clip(CapsuleShape)
                                .background(Color.White.copy(alpha = 0.20f))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Header Bar: Minimal Avatar, Title + Version + Verified Badge, Close
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sleek avatar
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                brandColor.copy(alpha = 0.22f),
                                                brandColor.copy(alpha = 0.06f)
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.30f),
                                                brandColor.copy(alpha = 0.20f)
                                            )
                                        ),
                                        RoundedCornerShape(13.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = extension.name.take(2).uppercase(),
                                    color = brandColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = extension.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Minimal Verified / Trust Indicator Pill
                                    when (extension.trustLevel) {
                                        ExtensionTrustLevel.OFFICIAL -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(CapsuleShape)
                                                    .background(Color(0xFF00DC64).copy(alpha = 0.10f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF00DC64),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Verified",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF00DC64)
                                                )
                                            }
                                        }
                                        ExtensionTrustLevel.COMMUNITY -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(CapsuleShape)
                                                    .background(Color(0xFF38BDF8).copy(alpha = 0.10f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Shield,
                                                    contentDescription = null,
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Community",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF38BDF8)
                                                )
                                            }
                                        }
                                        ExtensionTrustLevel.UNTRUSTED -> {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(CapsuleShape)
                                                    .background(Color(0xFFEF4444).copy(alpha = 0.10f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Security,
                                                    contentDescription = null,
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Untrusted",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "v${extension.versionName} • ${extension.lang}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            PanelCircleIconButton(
                                icon = Icons.Default.Close,
                                size = 34.dp,
                                iconSize = 16.dp,
                                backgroundColor = Color.White.copy(alpha = 0.06f),
                                borderColor = Color.White.copy(alpha = 0.12f),
                                onClick = onDismiss
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Scrollable Body
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Description
                            if (extension.description.isNotBlank()) {
                                Text(
                                    text = extension.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Minimal Grouped Specs Card (Apple / VisionOS style)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.035f))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    // Row 1: Source Providers
                                    if (extension.sources.isNotEmpty()) {
                                        val sourcesText = extension.sources.joinToString(", ") { it.name }
                                        MinimalInfoRow(
                                            label = "Sources",
                                            value = sourcesText
                                        )
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                                    }

                                    // Row 2: Whitelisted Host / Network
                                    val domainText = extension.networkDomains.firstOrNull() ?: extension.sources.firstOrNull()?.baseUrl?.substringAfter("://")?.substringBefore('/') ?: "None"
                                    MinimalInfoRow(
                                        label = "Host",
                                        value = domainText
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                                    // Row 3: Cache Storage
                                    val cacheMb = extension.cachedBytes / (1024 * 1024)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 11.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Storage",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextTertiary,
                                            fontSize = 13.sp
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (cacheMb > 0) "$cacheMb MB" else "Zero cache",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                            if (cacheMb > 0) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Clear",
                                                    color = Color(0xFFEF4444),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier
                                                        .clip(CapsuleShape)
                                                        .clickable { onClearCache(extension) }
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)

                                    // Row 4: Package identifier (clean mono)
                                    MinimalInfoRow(
                                        label = "Package",
                                        value = extension.pkgName.substringAfterLast("."),
                                        subValue = extension.pkgName
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // Bottom Actions
                        when (extension.status) {
                            ExtensionStatus.UPDATE_AVAILABLE -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PrimarySheetButton(
                                        text = "Update (${extension.updateVersion ?: ""})",
                                        backgroundColor = Color.White,
                                        contentColor = Color(0xFF090D15),
                                        modifier = Modifier.weight(1f),
                                        onClick = { onUpdate(extension) }
                                    )
                                    SecondarySheetButton(
                                        text = "Uninstall",
                                        modifier = Modifier.weight(0.6f),
                                        onClick = { onUninstall(extension) }
                                    )
                                }
                            }
                            ExtensionStatus.AVAILABLE -> {
                                PrimarySheetButton(
                                    text = "Install Extension",
                                    backgroundColor = Color.White,
                                    contentColor = Color(0xFF090D15),
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onInstall(extension) }
                                )
                            }
                            ExtensionStatus.INSTALLED -> {
                                if (onBrowse != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        PrimarySheetButton(
                                            text = "Browse Source",
                                            backgroundColor = Color.White,
                                            contentColor = Color(0xFF090D15),
                                            modifier = Modifier.weight(1f),
                                            onClick = { onBrowse(extension) }
                                        )
                                        SecondarySheetButton(
                                            text = "Uninstall",
                                            modifier = Modifier.weight(0.6f),
                                            onClick = { onUninstall(extension) }
                                        )
                                    }
                                } else {
                                    SecondarySheetButton(
                                        text = "Uninstall Extension",
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { onUninstall(extension) }
                                    )
                                }
                            }
                            ExtensionStatus.INSTALLING -> {
                                PrimarySheetButton(
                                    text = "Installing ${(extension.installProgress * 100).toInt()}%...",
                                    backgroundColor = Color.White.copy(alpha = 0.15f),
                                    contentColor = Color.White,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {}
                                )
                            }
                            ExtensionStatus.UNINSTALLING -> {
                                PrimarySheetButton(
                                    text = "Uninstalling...",
                                    backgroundColor = Color.White.copy(alpha = 0.15f),
                                    contentColor = Color.White,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {}
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalInfoRow(
    label: String,
    value: String,
    subValue: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            fontSize = 13.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PrimarySheetButton(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SecondarySheetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.90f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

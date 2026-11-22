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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import core.extensions.js.JsExtensionManifest
import data.extension.ExtensionItem
import data.extension.ExtensionRepository
import kotlinx.coroutines.launch
import ui.theme.ElectricBlue
import ui.theme.ElectricBlueLight
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.TextMuted
import ui.theme.TextPrimary
import ui.theme.TextSecondary
import ui.theme.TextTertiary

private enum class SideloadMode {
    FROM_URL,
    PASTE_SCRIPT
}

@Composable
fun SideloadExtensionDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    repository: ExtensionRepository,
    onExtensionInstalled: (ExtensionItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val coroutineScope = rememberCoroutineScope()
    var selectedMode by remember { mutableStateOf(SideloadMode.FROM_URL) }

    var scriptUrl by remember { mutableStateOf("") }
    var rawScript by remember { mutableStateOf("") }

    var isInstalling by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Live manifest extraction for paste script
    val parsedManifest: JsExtensionManifest? = remember(rawScript) {
        if (rawScript.isBlank()) null
        else repository.parseExtensionManifest(rawScript)
    }

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
                    .heightIn(max = 640.dp)
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
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sideload Extension",
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
                        text = "Install custom JavaScript extensions directly. Sideloaded extensions run in an isolated JS sandbox with network access to their base URL.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mode Tab Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selectedMode == SideloadMode.FROM_URL) ElectricBlue else Color.Transparent)
                                .clickable {
                                    selectedMode = SideloadMode.FROM_URL
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "From URL",
                                fontSize = 13.sp,
                                fontWeight = if (selectedMode == SideloadMode.FROM_URL) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedMode == SideloadMode.FROM_URL) Color.White else TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selectedMode == SideloadMode.PASTE_SCRIPT) ElectricBlue else Color.Transparent)
                                .clickable {
                                    selectedMode = SideloadMode.PASTE_SCRIPT
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Paste Script",
                                fontSize = 13.sp,
                                fontWeight = if (selectedMode == SideloadMode.PASTE_SCRIPT) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedMode == SideloadMode.PASTE_SCRIPT) Color.White else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (selectedMode) {
                        SideloadMode.FROM_URL -> {
                            Text(
                                text = "Script URL (.js)",
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
                                if (scriptUrl.isEmpty()) {
                                    Text(
                                        text = "https://example.com/extension.bundle.js",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted
                                    )
                                }
                                BasicTextField(
                                    value = scriptUrl,
                                    onValueChange = {
                                        scriptUrl = it
                                        errorMessage = null
                                    },
                                    textStyle = TextStyle(
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    ),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }

                        SideloadMode.PASTE_SCRIPT -> {
                            Text(
                                text = "JavaScript Bundle Content",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextTertiary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                if (rawScript.isEmpty()) {
                                    Text(
                                        text = "// Paste extension script with manifest definition:\nmanifest = {\n  id: \"my-source\",\n  name: \"My Manga Source\",\n  version: \"1.0.0\",\n  baseUrl: \"https://...\"\n};",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                BasicTextField(
                                    value = rawScript,
                                    onValueChange = {
                                        rawScript = it
                                        errorMessage = null
                                    },
                                    textStyle = TextStyle(
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    ),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Live Manifest Preview Card
                            if (parsedManifest != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF00DC64).copy(alpha = 0.08f))
                                        .border(1.dp, Color(0xFF00DC64).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF00DC64),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Valid Manifest Detected",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00DC64)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${parsedManifest.name} (v${parsedManifest.version}) • ${parsedManifest.lang.uppercase()}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Base: ${parsedManifest.baseUrl}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Security Sandbox Advisory Card
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.08f))
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp).padding(top = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Security Sandbox: Sideloaded extensions are tagged as UNOFFICIAL. Only install extensions from authors you trust.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color(0xFFF59E0B),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Error Message Feedback
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Success Feedback
                    if (successMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00DC64),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = successMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF00DC64),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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

                        val canInstall = when (selectedMode) {
                            SideloadMode.FROM_URL -> scriptUrl.isNotBlank() && !isInstalling
                            SideloadMode.PASTE_SCRIPT -> rawScript.isNotBlank() && parsedManifest != null && !isInstalling
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (canInstall) ElectricBlue else Color.White.copy(alpha = 0.20f))
                                .clickable(enabled = canInstall) {
                                    isInstalling = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        val result = when (selectedMode) {
                                            SideloadMode.FROM_URL -> repository.fetchAndSideloadExtension(scriptUrl.trim())
                                            SideloadMode.PASTE_SCRIPT -> repository.sideloadExtension(rawScript.trim())
                                        }

                                        isInstalling = false
                                        result.fold(
                                            onSuccess = { item ->
                                                successMessage = "Installed ${item.name} successfully!"
                                                onExtensionInstalled(item)
                                                onDismiss()
                                            },
                                            onFailure = { err ->
                                                errorMessage = err.message ?: "Failed to sideload extension"
                                            }
                                        )
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isInstalling) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Install Extension",
                                    color = if (canInstall) Color.White else Color.White.copy(alpha = 0.40f),
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
}

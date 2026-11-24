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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.extensions.js.JsExtensionManifest
import data.extension.ExtensionItem
import data.extension.ExtensionRepository
import kotlinx.coroutines.launch
import ui.theme.CapsuleShape
import ui.theme.GlassBorder
import ui.theme.GlassBorderStrong
import ui.theme.PanelSurfaceGlassHigh
import ui.theme.TextMuted
import ui.theme.TextPrimary
import ui.theme.TextSecondary
import ui.theme.TextTertiary

private enum class SideloadMode {
    FROM_URL,
    PASTE_SCRIPT
}

/**
 * Minimal, modern frosted dialog for sideloading JavaScript extensions.
 */
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
    val primaryColor = MaterialTheme.colorScheme.primary
    var selectedMode by remember { mutableStateOf(SideloadMode.FROM_URL) }

    var scriptUrl by remember { mutableStateOf("") }
    var rawScript by remember { mutableStateOf("") }

    var isInstalling by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

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
            val dialogShape = RoundedCornerShape(22.dp)
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .clip(dialogShape)
                    .background(Color(0xF012151D))
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
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sideload Extension",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Minimal Mode Segmented Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CapsuleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CapsuleShape)
                                .background(if (selectedMode == SideloadMode.FROM_URL) primaryColor else Color.Transparent)
                                .clickable {
                                    selectedMode = SideloadMode.FROM_URL
                                    errorMessage = null
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "From URL",
                                fontSize = 12.sp,
                                fontWeight = if (selectedMode == SideloadMode.FROM_URL) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedMode == SideloadMode.FROM_URL) Color.White else TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CapsuleShape)
                                .background(if (selectedMode == SideloadMode.PASTE_SCRIPT) primaryColor else Color.Transparent)
                                .clickable {
                                    selectedMode = SideloadMode.PASTE_SCRIPT
                                    errorMessage = null
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Paste Script",
                                fontSize = 12.sp,
                                fontWeight = if (selectedMode == SideloadMode.PASTE_SCRIPT) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedMode == SideloadMode.PASTE_SCRIPT) Color.White else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (selectedMode) {
                        SideloadMode.FROM_URL -> {
                            Text(
                                text = "Extension Script URL",
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
                                    .border(0.75.dp, GlassBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 11.dp)
                            ) {
                                if (scriptUrl.isEmpty()) {
                                    Text(
                                        text = "https://example.com/extension.bundle.js",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted,
                                        fontSize = 13.sp
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
                                        fontSize = 13.sp
                                    ),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }

                        SideloadMode.PASTE_SCRIPT -> {
                            Text(
                                text = "JavaScript Bundle Source",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextTertiary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(0.75.dp, GlassBorder, RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                if (rawScript.isEmpty()) {
                                    Text(
                                        text = "// Paste extension script with manifest definition:\nmanifest = { id: \"...\", name: \"...\", version: \"1.0.0\" };",
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

                            // Minimal Manifest Badge
                            if (parsedManifest != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${parsedManifest.name} • v${parsedManifest.version} • ${parsedManifest.lang.uppercase()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }

                    // Minimal Sandbox Hint
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sandboxed execution. Only install extensions from trusted authors.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }

                    // Error Message Feedback
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Success Feedback
                    if (successMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = successMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF10B981),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Minimal Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .clickable { onDismiss() }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = TextSecondary,
                                fontSize = 13.sp,
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
                                .background(if (canInstall) primaryColor else Color.White.copy(alpha = 0.15f))
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
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isInstalling) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Install",
                                    color = if (canInstall) Color.White else Color.White.copy(alpha = 0.40f),
                                    fontSize = 13.sp,
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

package ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.glass.GlassQuality
import ui.glass.GlassSurfaceContext
import ui.glass.PanelGlassState
import ui.theme.BottomSheetShape
import ui.theme.LocalGlassTokens

/**
 * Standardized Glass bottom sheet wrapper using [BottomSheetShape] (24.dp top corners).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    glassState: PanelGlassState? = null,
    forcedQuality: GlassQuality? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalGlassTokens.current
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        shape = BottomSheetShape,
        modifier = modifier
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = BottomSheetShape,
            style = GlassStyle.SHEET,
            context = GlassSurfaceContext.STANDARD,
            glassState = glassState,
            forcedQuality = forcedQuality
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(tokens.subtextOnGlass.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.height(14.dp))
                content()
            }
        }
    }
}

/**
 * Standardized Glass confirmation/content dialog using [GlassTokens.shapeLg] (24.dp).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
    glassState: PanelGlassState? = null,
    forcedQuality: GlassQuality? = null,
    body: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalGlassTokens.current
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = tokens.shapeLg,
            style = GlassStyle.SHEET,
            context = GlassSurfaceContext.STANDARD,
            glassState = glassState,
            forcedQuality = forcedQuality
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Text(
                    text = title,
                    color = tokens.textOnGlass,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                body()
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassButton(
                        text = dismissText,
                        onClick = onDismissRequest,
                        variant = GlassButtonVariant.OUTLINE,
                        forcedQuality = forcedQuality
                    )
                    GlassButton(
                        text = confirmText,
                        onClick = onConfirm,
                        variant = if (isDestructive) GlassButtonVariant.DESTRUCTIVE else GlassButtonVariant.PRIMARY,
                        forcedQuality = forcedQuality
                    )
                }
            }
        }
    }
}

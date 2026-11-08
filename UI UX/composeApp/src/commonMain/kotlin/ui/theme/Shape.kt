package ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val PanelShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val CapsuleShape = RoundedCornerShape(999.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val GlassCardShape = RoundedCornerShape(16.dp)
val CoverImageShape = RoundedCornerShape(12.dp)

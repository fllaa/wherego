package app.wherego.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object WheregoShapes {
    val goAvatar = 27.dp
    val pill = 99.dp
    val card = 28.dp
    val sheetTop = 36.dp
    val kindToggle = 18.dp
    val numpadKey = 18.dp
    val save = 20.dp
    val txBadge = 16.dp

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(card),
        extraLarge = RoundedCornerShape(sheetTop),
    )
}

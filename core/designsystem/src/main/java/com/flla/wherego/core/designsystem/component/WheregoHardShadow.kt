package com.flla.wherego.core.designsystem.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flla.wherego.core.designsystem.theme.WheregoTheme

/**
 * The sticker shadow every raised surface in `pencil-new.pen` carries:
 * `{ type: shadow, shadowType: outer, color: $ink, offset: { x: 0, y: 4|5 } }` with no blur and
 * no spread — a solid ink copy of the shape pushed straight down.
 *
 * Deliberately not `Modifier.shadow()`: that draws a blurred elevation shadow tinted by the
 * Material scheme, which is the opposite of this flat illustrated look.
 *
 * Apply BEFORE `clip`/`background` so the offset copy paints underneath the surface.
 */
fun Modifier.wheregoHardShadow(
    shape: Shape,
    color: Color,
    offsetY: Dp = 4.dp,
): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    translate(top = offsetY.toPx()) {
        drawOutline(outline = outline, color = color)
    }
}

/** [wheregoHardShadow] with the themed ink shadow colour. */
@Composable
fun Modifier.wheregoHardShadow(
    cornerRadius: Dp,
    offsetY: Dp = 4.dp,
): Modifier = wheregoHardShadow(
    shape = RoundedCornerShape(cornerRadius),
    color = WheregoTheme.colors.shadow,
    offsetY = offsetY,
)

package com.flla.wherego.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flla.wherego.core.designsystem.theme.WheregoTheme

/**
 * The Waypoint Coin brand mark.
 * Rendered using Canvas so it dynamically responds to [WheregoTheme.colors] across light & dark modes.
 */
@Composable
fun WheregoWaypointMark(
    modifier: Modifier = Modifier,
    size: Dp? = null,
    baseFillColor: Color? = null,
) {
    val colors = WheregoTheme.colors
    val paperColor = baseFillColor ?: colors.paper
    val inkColor = colors.ink
    val tealColor = colors.teal
    val tealDeepColor = colors.tealDeep
    val mascotFillColor = colors.mascotFill
    val tealSoftColor = colors.tealSoft
    val coralColor = colors.coral
    val mutedColor = colors.muted

    val boxModifier = if (size != null) modifier.size(size) else modifier.aspectRatio(1f)

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height
            val scale = (w.coerceAtMost(h)) / 100f

            fun cX(x: Float) = x * scale
            fun cY(y: Float) = y * scale

            val strokeWidthOuter = 4f * scale
            val strokeWidthInner = 2.5f * scale
            val strokeWidthNeedle = 2.5f * scale
            val strokeWidthPip = 2f * scale

            val center = Offset(cX(50f), cY(50f))

            // 1. Base Coin Disc
            drawCircle(
                color = paperColor,
                radius = 41f * scale,
                center = center,
                style = Fill,
            )
            drawCircle(
                color = inkColor,
                radius = 41f * scale,
                center = center,
                style = Stroke(
                    width = strokeWidthOuter,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            // 2. Inner Ledger Rim (Dashed track)
            val dashIntervals = floatArrayOf(3f * scale, 3.5f * scale)
            drawCircle(
                color = mascotFillColor,
                radius = 33.5f * scale,
                center = center,
                style = Fill,
            )
            drawCircle(
                color = inkColor,
                radius = 33.5f * scale,
                center = center,
                style = Stroke(
                    width = strokeWidthInner,
                    pathEffect = PathEffect.dashPathEffect(dashIntervals, 0f),
                ),
            )

            // 3. Secondary Waypoint Pips (NW & SE)
            val nwPath = Path().apply {
                moveTo(cX(50f), cY(50f))
                lineTo(cX(36f), cY(36f))
                lineTo(cX(46f), cY(41f))
                close()
            }
            drawPath(nwPath, tealSoftColor, style = Fill)
            drawPath(nwPath, inkColor, style = Stroke(width = strokeWidthPip, join = StrokeJoin.Round))

            val sePath = Path().apply {
                moveTo(cX(50f), cY(50f))
                lineTo(cX(64f), cY(64f))
                lineTo(cX(54f), cY(59f))
                close()
            }
            drawPath(sePath, tealSoftColor, style = Fill)
            drawPath(sePath, inkColor, style = Stroke(width = strokeWidthPip, join = StrokeJoin.Round))

            // 4. South-West Anchor (Ink & Muted)
            val swPath = Path().apply {
                moveTo(cX(50f), cY(50f))
                lineTo(cX(29f), cY(71f))
                lineTo(cX(48f), cY(66f))
                close()
            }
            drawPath(swPath, inkColor, style = Fill)
            drawPath(swPath, inkColor, style = Stroke(width = strokeWidthNeedle, join = StrokeJoin.Round))

            val swShadePath = Path().apply {
                moveTo(cX(50f), cY(50f))
                lineTo(cX(34f), cY(52f))
                lineTo(cX(29f), cY(71f))
                close()
            }
            drawPath(swShadePath, mutedColor, style = Fill)
            drawPath(swShadePath, inkColor, style = Stroke(width = strokeWidthNeedle, join = StrokeJoin.Round))

            // 5. North-East Pointer (Active Cobalt & Deep Cobalt)
            val nePath = Path().apply {
                moveTo(cX(50f), cY(50f))
                lineTo(cX(71f), cY(29f))
                lineTo(cX(52f), cY(34f))
                close()
            }
            drawPath(nePath, tealColor, style = Fill)
            drawPath(nePath, inkColor, style = Stroke(width = strokeWidthNeedle, join = StrokeJoin.Round))

            val neShadePath = Path().apply {
                moveTo(cX(50f), cY(50f))
                lineTo(cX(66f), cY(48f))
                lineTo(cX(71f), cY(29f))
                close()
            }
            drawPath(neShadePath, tealDeepColor, style = Fill)
            drawPath(neShadePath, inkColor, style = Stroke(width = strokeWidthNeedle, join = StrokeJoin.Round))

            // 6. Center Axis Coral Pivot Pin
            drawCircle(
                color = coralColor,
                radius = 6.5f * scale,
                center = center,
                style = Fill,
            )
            drawCircle(
                color = inkColor,
                radius = 6.5f * scale,
                center = center,
                style = Stroke(width = strokeWidthNeedle),
            )
            // Center highlight pip
            drawCircle(
                color = Color.White,
                radius = 2f * scale,
                center = center,
                style = Fill,
            )
        }
    }
}

package com.mux.compass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/** A marker on the dial. [angle] is a bearing in the same frame as the dial heading. */
class DialMarker(val angle: () -> Float, val color: Color, val label: String, val line: Boolean = false)

/**
 * Rotating compass dial. [heading] is read inside the draw phase, so the dial redraws
 * on every sensor update without recomposing anything.
 */
@Composable
fun Dial(
    heading: () -> Float,
    modifier: Modifier = Modifier,
    markers: List<DialMarker> = emptyList(),
    center: @Composable BoxScope.() -> Unit = {}
) {
    val main = cMain
    val dim = cDim
    val paint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
            isFakeBoldText = true
        }
    }
    Box(modifier.fillMaxWidth().aspectRatio(1f), Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f * 0.92f
            val sw = 1.5.dp.toPx()
            val h = heading()
            drawCircle(dim, r, c, style = Stroke(2.dp.toPx()))
            drawCircle(dim.copy(alpha = 0.5f), r * 0.62f, c, style = Stroke(1.dp.toPx()))
            drawCircle(dim.copy(alpha = 0.3f), r * 0.3f, c, style = Stroke(1.dp.toPx()))
            val ts = r * 0.085f
            paint.textSize = ts
            rotate(-h, c) {
                for (i in 0 until 72) {
                    val deg = i * 5
                    val cardinal = deg % 90 == 0
                    val major = deg % 30 == 0
                    val len = if (cardinal) r * 0.12f else if (major) r * 0.08f else r * 0.04f
                    rotate(deg.toFloat(), c) {
                        drawLine(
                            if (deg == 0) cBad else main.copy(alpha = if (major) 1f else 0.55f),
                            Offset(c.x, c.y - r), Offset(c.x, c.y - r + len),
                            if (major) sw * 1.6f else sw
                        )
                        if (major) {
                            paint.color = (if (deg == 0) cBad else if (cardinal) main else main.copy(alpha = 0.75f)).toArgb()
                            val label = when (deg) { 0 -> "N"; 90 -> "E"; 180 -> "S"; 270 -> "W"; else -> "$deg" }
                            drawContext.canvas.nativeCanvas.drawText(label, c.x, c.y - r + len + ts * 1.2f, paint)
                        }
                    }
                }
                for (m in markers) {
                    rotate(m.angle(), c) {
                        if (m.line) drawLine(m.color.copy(alpha = 0.55f), c, Offset(c.x, c.y - r), 2.dp.toPx())
                        val p = Path().apply {
                            moveTo(c.x, c.y - r - 6.dp.toPx())
                            lineTo(c.x - 7.dp.toPx(), c.y - r + 10.dp.toPx())
                            lineTo(c.x + 7.dp.toPx(), c.y - r + 10.dp.toPx())
                            close()
                        }
                        drawPath(p, m.color)
                    }
                }
            }
            // fixed pointer at the top: the direction the phone is pointing
            val p = Path().apply {
                moveTo(c.x - 9.dp.toPx(), c.y - r - 14.dp.toPx())
                lineTo(c.x + 9.dp.toPx(), c.y - r - 14.dp.toPx())
                lineTo(c.x, c.y - r + 2.dp.toPx())
                close()
            }
            drawPath(p, main)
            // crosshair
            val a = 10.dp.toPx()
            drawLine(dim, Offset(c.x - a, c.y), Offset(c.x + a, c.y), sw)
            drawLine(dim, Offset(c.x, c.y - a), Offset(c.x, c.y + a), sw)
        }
        center()
    }
}

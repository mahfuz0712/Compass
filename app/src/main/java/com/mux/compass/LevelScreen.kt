package com.mux.compass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
fun LevelScreen() {
    var edge by remember { mutableStateOf(false) }
    var zx by remember { mutableFloatStateOf(0f) }
    var zy by remember { mutableFloatStateOf(0f) }
    val main = cMain
    val dim = cDim
    val good = cGood

    // slow values for the text readout
    val tx = (if (edge) Geo.edgeTilt(Compass.sgx, Compass.sgy, Compass.sgz) else Geo.tiltX(Compass.sgx, Compass.sgz)) - zx
    val ty = Geo.tiltY(Compass.sgy, Compass.sgz) - zy
    val ok = if (edge) abs(tx) < 1f else abs(tx) < 1f && abs(ty) < 1f

    Page {
        T("SPIRIT LEVEL", 16f, cMain, true)
        TwoCol(
            {
                Panel(if (edge) "EDGE LEVEL" else "SURFACE LEVEL") {
                    Canvas(Modifier.fillMaxWidth().aspectRatio(if (edge) 3f else 1f)) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val sw = 1.5.dp.toPx()
                        if (!edge) {
                            val r = size.minDimension / 2f * 0.92f
                            drawCircle(dim, r, c, style = Stroke(2.dp.toPx()))
                            drawCircle(dim.copy(alpha = 0.6f), r * 0.66f, c, style = Stroke(sw))
                            drawCircle(dim.copy(alpha = 0.6f), r * 0.33f, c, style = Stroke(sw))
                            drawLine(dim, Offset(c.x - r, c.y), Offset(c.x + r, c.y), sw)
                            drawLine(dim, Offset(c.x, c.y - r), Offset(c.x, c.y + r), sw)
                            val x = Geo.tiltX(Compass.gx, Compass.gz) - zx
                            val y = Geo.tiltY(Compass.gy, Compass.gz) - zy
                            val k = r / 20f
                            var bx = x * k
                            var by = -y * k
                            val len = sqrt(bx * bx + by * by)
                            if (len > r * 0.88f) {
                                bx *= r * 0.88f / len
                                by *= r * 0.88f / len
                            }
                            val col = if (abs(x) < 1f && abs(y) < 1f) good else main
                            val br = r * 0.11f
                            drawCircle(col.copy(alpha = 0.35f), br, Offset(c.x + bx, c.y + by))
                            drawCircle(col, br, Offset(c.x + bx, c.y + by), style = Stroke(2.dp.toPx()))
                        } else {
                            val w = size.width * 0.92f
                            val h = size.height * 0.5f
                            val left = c.x - w / 2f
                            val top = c.y - h / 2f
                            drawRoundRect(dim, Offset(left, top), Size(w, h), CornerRadius(h / 2f), style = Stroke(2.dp.toPx()))
                            drawLine(dim, Offset(c.x, top), Offset(c.x, top + h), sw)
                            drawLine(dim.copy(alpha = 0.6f), Offset(c.x - w * 0.1f, top), Offset(c.x - w * 0.1f, top + h), sw)
                            drawLine(dim.copy(alpha = 0.6f), Offset(c.x + w * 0.1f, top), Offset(c.x + w * 0.1f, top + h), sw)
                            val a = Geo.edgeTilt(Compass.gx, Compass.gy, Compass.gz) - zx
                            val k = (w / 2f) / 20f
                            val bx = (a * k).coerceIn(-w / 2f + h / 2f, w / 2f - h / 2f)
                            val col = if (abs(a) < 1f) good else main
                            drawCircle(col.copy(alpha = 0.35f), h * 0.4f, Offset(c.x + bx, c.y))
                            drawCircle(col, h * 0.4f, Offset(c.x + bx, c.y), style = Stroke(2.dp.toPx()))
                        }
                    }
                    T(
                        if (ok) "LEVEL" else "NOT LEVEL",
                        20f, if (ok) cGood else cWarn, true,
                        Modifier.fillMaxWidth().padding(top = 10.dp), align = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            {
                Panel("READOUT") {
                    if (edge) {
                        KV("TILT", "${f1(tx)}°", if (abs(tx) < 1f) cGood else cWarn)
                    } else {
                        KV("TILT LEFT / RIGHT", "${f1(tx)}°", if (abs(tx) < 1f) cGood else cWarn)
                        KV("TILT FRONT / BACK", "${f1(ty)}°", if (abs(ty) < 1f) cGood else cWarn)
                    }
                    T(
                        if (edge) "EDGE: stand the phone on its long edge against a wall or shelf."
                        else "SURFACE: lay the phone flat on the table or object you want to check.",
                        10f, cSub, lines = 3, mod = Modifier.padding(vertical = 8.dp)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip(if (edge) "SURFACE MODE" else "EDGE MODE", false, Modifier.weight(1f)) { edge = !edge; zx = 0f; zy = 0f }
                        Chip("ZERO HERE", false, Modifier.weight(1f)) {
                            zx += tx
                            zy += ty
                        }
                        Chip("RESET", false, Modifier.weight(1f)) { zx = 0f; zy = 0f }
                    }
                    T("ZERO HERE sets the current position as level, useful to calibrate on a surface you know is flat.", 9f, cSub, lines = 3, mod = Modifier.padding(top = 8.dp))
                }
            }
        )
    }
}

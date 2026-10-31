package com.mux.compass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

fun accuracyText(a: Int): String = when (a) {
    3 -> "HIGH"
    2 -> "MEDIUM"
    1 -> "LOW"
    0 -> "UNRELIABLE"
    else -> "UNKNOWN"
}

fun accuracyColor(a: Int): Color = when (a) {
    3, 2 -> cGood
    1 -> cWarn
    0 -> cBad
    else -> cMain
}

@Composable
fun CompassScreen() {
    var trueMode by remember { mutableStateOf(false) }
    var hold by remember { mutableStateOf<Float?>(null) }
    var mark by remember { mutableStateOf<Float?>(null) }

    val loc = Loc.location
    val useTrue = trueMode && loc != null
    val off = if (useTrue) Loc.declination else 0f
    val held = hold
    val shown = held ?: Geo.norm360((Compass.sHeading + off).toDouble()).toFloat()
    val markVal = mark
    val markers = if (markVal != null) listOf(DialMarker({ markVal }, cWarn, "MARK", true)) else emptyList()

    Page {
        T("COMPASS", 16f, cMain, true)
        if (!Compass.hasCompass) {
            Panel("NO COMPASS SENSOR") {
                T("This device has no magnetometer, so a compass is not possible. GPS, Qibla math and the level still work.", 11f, cBad, lines = 5)
            }
        }
        TwoCol(
            {
                Panel("HEADING") {
                    Dial(
                        heading = { held ?: Geo.norm360((Compass.heading + off).toDouble()).toFloat() },
                        markers = markers,
                        center = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                T("${f0(shown)}°", 34f, Color.White, true)
                                T(Geo.cardinal(shown.toDouble()), 16f, if (held != null) cWarn else cMain, true)
                                if (held != null) T("HELD", 10f, cWarn)
                            }
                        }
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip(if (trueMode) "TRUE NORTH" else "MAGNETIC", trueMode, Modifier.weight(1f)) {
                            trueMode = !trueMode
                            mark = null
                        }
                        Chip(if (held != null) "RELEASE" else "HOLD", held != null, Modifier.weight(1f)) {
                            hold = if (held != null) null else shown
                        }
                        Chip(if (markVal != null) "CLEAR MARK" else "MARK", markVal != null, Modifier.weight(1f)) {
                            mark = if (markVal != null) null else shown
                        }
                    }
                }
            },
            {
                Panel("READOUT") {
                    KV("HEADING", "${f1(shown)}°  ${Geo.cardinal(shown.toDouble())}")
                    KV("NORTH", if (useTrue) "TRUE (declination ${if (off >= 0) "+" else ""}${f1(off)}°)" else "MAGNETIC")
                    if (trueMode && loc == null) {
                        T("True north needs your location. Grant it in the SYS tab.", 10f, cWarn, lines = 2)
                    }
                    KV("PITCH / ROLL", "${f0(Compass.sPitch)}° / ${f0(Compass.sRoll)}°")
                    if (markVal != null) {
                        val d = Geo.diff(markVal.toDouble(), shown.toDouble()).toFloat()
                        KV(
                            "MARKED BEARING",
                            "${f0(markVal)}°  " + (if (abs(d) < 2f) "ON TARGET" else if (d > 0) "TURN RIGHT ${f0(abs(d))}°" else "TURN LEFT ${f0(abs(d))}°"),
                            if (abs(d) < 2f) cGood else cWarn
                        )
                    }
                }
                Panel("MAGNETIC FIELD") {
                    val f = Compass.sField
                    val (txt, col) = when {
                        f <= 0.1f -> "NO DATA" to cSub
                        f > 65f -> "STRONG: move away from magnets and metal" to cBad
                        f < 20f -> "WEAK: unusual, check the sensor" to cWarn
                        else -> "NORMAL (earth is about 25 to 65 µT)" to cGood
                    }
                    KV("STRENGTH", "${f1(f)} µT", col)
                    Bar(f / 100f, col, Modifier.padding(vertical = 6.dp))
                    T(txt, 10f, col, lines = 2)
                    KV("SENSOR ACCURACY", accuracyText(Compass.accuracy), accuracyColor(Compass.accuracy))
                    if (Compass.accuracy in 0..1) {
                        T(
                            "Calibrate: move the phone in a figure-8 motion a few times, away from cases with magnets.",
                            10f, cWarn, lines = 3, mod = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        )
    }
}

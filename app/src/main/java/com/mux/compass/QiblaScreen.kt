package com.mux.compass

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun QiblaScreen() {
    val ctx = LocalContext.current
    val ptick = Perms.tick
    val has = remember(ptick) { Perms.hasLocation(ctx) }
    val loc = Loc.location

    Page {
        T("QIBLA DIRECTION", 16f, cMain, true)
        if (loc == null) {
            Panel("LOCATION NEEDED") {
                T("Qibla is the direction from where you are to the Kaaba in Makkah, so the app needs your position.", 11f, cSub, lines = 4)
                if (!has) {
                    Chip("GRANT LOCATION", false, Modifier.fillMaxWidth().padding(top = 10.dp)) { Perms.requestLocation() }
                } else {
                    T("WAITING FOR A LOCATION FIX...", 12f, cWarn, mod = Modifier.padding(top = 10.dp))
                }
            }
            return@Page
        }

        val bearing = Geo.qiblaBearing(loc.latitude, loc.longitude).toFloat()
        val decl = Loc.declination
        val trueHeading = Geo.norm360((Compass.sHeading + decl).toDouble())
        val delta = Geo.diff(bearing.toDouble(), trueHeading).toFloat()
        val aligned = abs(delta) < 3f
        LaunchedEffect(aligned) { if (aligned) buzz(ctx) }

        TwoCol(
            {
                Panel("QIBLA COMPASS") {
                    Dial(
                        heading = { Geo.norm360((Compass.heading + decl).toDouble()).toFloat() },
                        markers = listOf(DialMarker({ bearing }, cWarn, "KAABA", true)),
                        center = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                T("${f0(bearing)}°", 28f, Color.White, true)
                                T("TO KAABA", 11f, cWarn)
                            }
                        }
                    )
                    T(
                        if (aligned) "FACING QIBLA" else if (delta > 0) "TURN RIGHT ${f0(abs(delta))}°" else "TURN LEFT ${f0(abs(delta))}°",
                        18f, if (aligned) cGood else cWarn, true,
                        Modifier.fillMaxWidth().padding(top = 10.dp), align = TextAlign.Center
                    )
                }
            },
            {
                Panel("DETAILS") {
                    KV("QIBLA (TRUE NORTH)", "${f1(bearing)}°  ${Geo.cardinal(bearing.toDouble())}")
                    KV("QIBLA (MAGNETIC)", "${f1(Geo.norm360((bearing - decl).toDouble()).toFloat())}°")
                    KV("DISTANCE TO KAABA", "${f0(Geo.qiblaDistanceKm(loc.latitude, loc.longitude).toFloat())} km")
                    KV("YOUR POSITION", "${f6(loc.latitude)}, ${f6(loc.longitude)}")
                    KV("MAGNETIC DECLINATION", "${if (decl >= 0) "+" else ""}${f1(decl)}°")
                    KV("COMPASS ACCURACY", accuracyText(Compass.accuracy), accuracyColor(Compass.accuracy))
                    T(
                        "Hold the phone flat and away from metal and magnets. If the accuracy is LOW, move it in a figure-8 to calibrate. The phone vibrates once when you face Qibla.",
                        10f, cSub, lines = 6, mod = Modifier.padding(top = 8.dp)
                    )
                }
            }
        )
    }
}

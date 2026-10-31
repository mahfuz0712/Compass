package com.mux.compass

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GpsScreen() {
    val ctx = LocalContext.current
    val ptick = Perms.tick
    val has = remember(ptick) { Perms.hasLocation(ctx) }
    val loc = Loc.location
    var msg by remember { mutableStateOf("") }

    Page {
        T("GPS POSITION", 16f, cMain, true)
        if (!has) {
            Panel("PERMISSION") {
                T("Location permission is needed to show your coordinates.", 11f, cSub, lines = 3)
                Chip("GRANT LOCATION", false, Modifier.fillMaxWidth().padding(top = 10.dp)) { Perms.requestLocation() }
            }
            return@Page
        }
        if (loc == null) {
            Panel("SEARCHING") {
                T("Waiting for a location fix. Go outdoors with a clear view of the sky for GPS.", 11f, cWarn, lines = 3)
            }
            return@Page
        }
        val text = "${f6(loc.latitude)}, ${f6(loc.longitude)}"
        val link = "https://maps.google.com/?q=${f6(loc.latitude)},${f6(loc.longitude)}"
        TwoCol(
            {
                Panel("COORDINATES") {
                    KV("LATITUDE", f6(loc.latitude))
                    KV("LONGITUDE", f6(loc.longitude))
                    KV("LAT (DMS)", Geo.dms(loc.latitude, "N", "S"))
                    KV("LON (DMS)", Geo.dms(loc.longitude, "E", "W"))
                    KV("ACCURACY", "${f1(loc.accuracy)} m")
                    if (loc.hasAltitude()) KV("ALTITUDE", "${f1(loc.altitude.toFloat())} m")
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip("COPY", false, Modifier.weight(1f)) {
                            try {
                                (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                    .setPrimaryClip(ClipData.newPlainText("coordinates", text))
                                msg = "COPIED: $text"
                            } catch (e: Exception) { }
                        }
                        Chip("SHARE", false, Modifier.weight(1f)) {
                            try {
                                val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "$text\n$link")
                                ctx.startActivity(Intent.createChooser(i, "Share position").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            } catch (e: Exception) { }
                        }
                        Chip("OPEN MAP", false, Modifier.weight(1f)) {
                            try {
                                ctx.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("geo:${loc.latitude},${loc.longitude}?q=${loc.latitude},${loc.longitude}"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (e: Exception) { }
                        }
                    }
                    if (msg.isNotEmpty()) T(msg, 10f, cGood, mod = Modifier.padding(top = 6.dp))
                }
            },
            {
                Panel("MOTION AND SOURCE") {
                    KV("SPEED", if (loc.hasSpeed()) "${f1(loc.speed * 3.6f)} km/h" else "N/A")
                    KV("COURSE", if (loc.hasBearing()) "${f0(loc.bearing)}°  ${Geo.cardinal(loc.bearing.toDouble())}" else "N/A")
                    KV("PROVIDER", (loc.provider ?: "?").uppercase())
                    KV("FIX TIME", SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(loc.time)))
                    KV("MAGNETIC DECLINATION", "${if (Loc.declination >= 0) "+" else ""}${f1(Loc.declination)}°")
                    T(
                        "Declination is the angle between true north and magnetic north at your position. TRUE NORTH mode on the compass uses it.",
                        10f, cSub, lines = 4, mod = Modifier.padding(top = 8.dp)
                    )
                }
            }
        )
    }
}

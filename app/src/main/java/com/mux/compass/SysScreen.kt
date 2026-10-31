package com.mux.compass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SysScreen(onUpdateFound: (UpdateInfo) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    val ptick = Perms.tick
    val hasLoc = remember(ptick) { Perms.hasLocation(ctx) }
    Page {
        T("SYSTEM", 16f, cMain, true)
        TwoCol(
            {
                Panel("INTERFACE COLOR") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ACCENTS.forEachIndexed { i, a -> Chip(a.name, Prefs.accent == i, Modifier.weight(1f)) { Prefs.updateAccent(i) } }
                    }
                }
                Panel("PERMISSIONS") {
                    T("Location is used for true north (declination), Qibla and GPS. Nothing is uploaded.", 10f, cSub, lines = 3, mod = Modifier.padding(bottom = 8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        T("LOCATION", 11f, if (hasLoc) cText else cSub, mod = Modifier.weight(1f))
                        if (hasLoc) T("GRANTED", 11f, cGood, true) else Chip("GRANT", false, Modifier.padding(start = 8.dp)) { Perms.requestLocation() }
                    }
                }
            },
            {
                Panel("UPDATES") {
                    KV("INSTALLED VERSION", "v${appVersion(ctx)}")
                    Chip("CHECK FOR UPDATES", false, Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        scope.launch {
                            status = "CHECKING..."
                            if (!isOnline(ctx)) {
                                status = "NO INTERNET CONNECTION"
                                return@launch
                            }
                            val (ok, info) = checkUpdate(ctx)
                            status = when {
                                !ok -> "CHECK FAILED"
                                info == null -> "UP TO DATE"
                                else -> "NEW VERSION AVAILABLE: v${info.version}"
                            }
                            if (info != null) onUpdateFound(info)
                        }
                    }
                    if (status.isNotEmpty()) T(status, 11f, cSub, mod = Modifier.padding(top = 6.dp))
                }
                Panel("ABOUT") {
                    T("COMPASS", 18f, cMain, true)
                    T("v${appVersion(ctx)}  com.mux.compass", 10f, cSub, mod = Modifier.padding(bottom = 8.dp))
                    T("DEVELOPER", 10f, cSub)
                    T(AppInfo.DEV_NAME, 14f, cText, true, Modifier.padding(bottom = 6.dp))
                    T("EMAIL", 10f, cSub)
                    T(AppInfo.DEV_EMAIL, 13f, cMain, mod = Modifier.clickable { openMail(ctx, AppInfo.DEV_EMAIL) }.padding(bottom = 6.dp))
                    T("GITHUB", 10f, cSub)
                    T("github.com/${AppInfo.GITHUB_USER}", 13f, cMain, mod = Modifier.clickable { openUrl(ctx, "https://github.com/${AppInfo.GITHUB_USER}") }.padding(bottom = 6.dp))
                    T("MORE APPS", 10f, cSub)
                    T("zApps Store", 13f, cMain, mod = Modifier.clickable { openUrl(ctx, AppInfo.MORE_APPS) })
                }
            }
        )
    }
}

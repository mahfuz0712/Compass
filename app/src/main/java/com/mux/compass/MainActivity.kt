package com.mux.compass

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT)
        )
        setContent { App() }
    }
}

private val TABS = listOf("COMPASS", "QIBLA", "GPS", "LEVEL", "SYS")

@Composable
fun App() {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = LocalView.current
    var tab by remember { mutableIntStateOf(0) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { Perms.tick++ }
    SideEffect { Perms.launcher = { permLauncher.launch(it) } }

    val ptick = Perms.tick
    val hasLoc = remember(ptick) { Perms.hasLocation(ctx) }

    // keep the screen on while the compass is open
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // sensors and location run only while the app is visible
    DisposableEffect(owner, hasLoc) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> {
                    Compass.start(ctx)
                    if (hasLoc) Loc.start(ctx)
                }
                Lifecycle.Event.ON_STOP -> {
                    Compass.stop()
                    Loc.stop()
                }
                Lifecycle.Event.ON_RESUME -> Perms.tick++
                else -> {}
            }
        }
        owner.lifecycle.addObserver(obs)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            Compass.start(ctx)
            if (hasLoc) Loc.start(ctx)
        }
        onDispose {
            owner.lifecycle.removeObserver(obs)
            Compass.stop()
            Loc.stop()
        }
    }

    LaunchedEffect(Unit) {
        if (isOnline(ctx)) update = checkUpdate(ctx).second
    }

    update?.let { u ->
        UpdateDialog(
            u, appVersion(ctx),
            onUpdate = { openUrl(ctx, u.apkUrl); update = null },
            onCancel = { update = null }
        )
    }

    HudBackground {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Header()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    0 -> CompassScreen()
                    1 -> QiblaScreen()
                    2 -> GpsScreen()
                    3 -> LevelScreen()
                    else -> SysScreen { update = it }
                }
            }
            NavBar(tab) { tab = it }
        }
    }
}

@Composable
private fun Header() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            T("COMPASS", 20f, cMain, true, Modifier.weight(1f))
            val h = Compass.sHeading.toDouble()
            T("[ ${Geo.cardinal(h)} ${f0(h.toFloat())}° ]", 11f, cSub, align = TextAlign.End)
        }
        T("// MUX NAVIGATION CORE", 11f, cSub)
        Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(1.dp).background(cDim))
    }
}

@Composable
private fun NavBar(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF050B10)).padding(top = 10.dp, bottom = 6.dp)) {
        TABS.forEachIndexed { i, name ->
            Column(
                Modifier.weight(1f).clickable { onSelect(i) }.padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                T(name, 12f, if (i == selected) cMain else cSub, i == selected)
                Box(
                    Modifier.padding(top = 4.dp).width(28.dp).height(2.dp)
                        .background(if (i == selected) cMain else Color.Transparent)
                )
            }
        }
    }
}

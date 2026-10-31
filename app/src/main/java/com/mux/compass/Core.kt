package com.mux.compass

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import java.util.Locale

object Prefs {
    private lateinit var sp: SharedPreferences
    var accent by mutableIntStateOf(0)

    fun init(ctx: Context) {
        sp = ctx.applicationContext.getSharedPreferences("mux_compass", Context.MODE_PRIVATE)
        accent = sp.getInt("accent", 0)
    }

    fun updateAccent(i: Int) {
        accent = i
        sp.edit().putInt("accent", i).apply()
    }
}

/** Runtime permission helper. Screens read [tick] so they refresh after a grant. */
object Perms {
    var tick by mutableIntStateOf(0)
    var launcher: ((Array<String>) -> Unit)? = null

    val location = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasLocation(ctx: Context): Boolean = location.any {
        ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocation() { launcher?.invoke(location) }
}

fun f0(v: Float): String = String.format(Locale.US, "%.0f", v)
fun f1(v: Float): String = String.format(Locale.US, "%.1f", v)
fun f2(v: Float): String = String.format(Locale.US, "%.2f", v)
fun f6(v: Double): String = String.format(Locale.US, "%.6f", v)

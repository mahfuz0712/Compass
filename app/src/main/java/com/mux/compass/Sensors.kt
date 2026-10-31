package com.mux.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GeomagneticField
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Live orientation data.
 * "Fast" values (heading, gx, gy, gz) update every sensor event and must only be read inside draw lambdas.
 * "Slow" values (s...) update about 10 times per second and are safe to read in normal composables for text.
 */
object Compass {
    var heading by mutableFloatStateOf(0f)
    var gx by mutableFloatStateOf(0f)
    var gy by mutableFloatStateOf(0f)
    var gz by mutableFloatStateOf(9.8f)

    var sHeading by mutableFloatStateOf(0f)
    var sPitch by mutableFloatStateOf(0f)
    var sRoll by mutableFloatStateOf(0f)
    var sField by mutableFloatStateOf(0f)
    var sgx by mutableFloatStateOf(0f)
    var sgy by mutableFloatStateOf(0f)
    var sgz by mutableFloatStateOf(9.8f)
    var accuracy by mutableIntStateOf(-1)
    var hasCompass by mutableStateOf(true)

    private var sm: SensorManager? = null
    private var wm: WindowManager? = null
    private var haveRotVec = false
    private var haveAcc = false
    private var haveMag = false
    private val acc = FloatArray(3)
    private val mag = FloatArray(3)
    private val rm = FloatArray(9)
    private val out = FloatArray(9)
    private val ori = FloatArray(3)
    private var sx = 0.0
    private var sy = 0.0
    private var seeded = false
    private var gInit = false
    private var fieldF = 0f
    private var lastSlow = 0L
    private var pitch = 0f
    private var roll = 0f

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            when (e.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rm, e.values)
                    computeHeading()
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    if (!gInit) {
                        gx = e.values[0]; gy = e.values[1]; gz = e.values[2]
                        gInit = true
                    } else {
                        gx += 0.15f * (e.values[0] - gx)
                        gy += 0.15f * (e.values[1] - gy)
                        gz += 0.15f * (e.values[2] - gz)
                    }
                    System.arraycopy(e.values, 0, acc, 0, 3)
                    haveAcc = true
                    if (!haveRotVec && haveMag && SensorManager.getRotationMatrix(rm, null, acc, mag)) computeHeading()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(e.values, 0, mag, 0, 3)
                    haveMag = true
                    accuracy = e.accuracy
                    val f = sqrt(mag[0] * mag[0] + mag[1] * mag[1] + mag[2] * mag[2])
                    fieldF = if (fieldF == 0f) f else fieldF + 0.2f * (f - fieldF)
                }
            }
            slowTick()
        }

        override fun onAccuracyChanged(s: Sensor?, a: Int) {
            if (s != null && s.type == Sensor.TYPE_MAGNETIC_FIELD) accuracy = a
        }
    }

    @Suppress("DEPRECATION")
    private fun computeHeading() {
        val rot = try { wm?.defaultDisplay?.rotation ?: 0 } catch (e: Exception) { 0 }
        val ax: Int
        val ay: Int
        when (rot) {
            1 -> { ax = SensorManager.AXIS_Y; ay = SensorManager.AXIS_MINUS_X }
            2 -> { ax = SensorManager.AXIS_MINUS_X; ay = SensorManager.AXIS_MINUS_Y }
            3 -> { ax = SensorManager.AXIS_MINUS_Y; ay = SensorManager.AXIS_X }
            else -> { ax = SensorManager.AXIS_X; ay = SensorManager.AXIS_Y }
        }
        SensorManager.remapCoordinateSystem(rm, ax, ay, out)
        SensorManager.getOrientation(out, ori)
        val az = ori[0].toDouble()
        pitch = Math.toDegrees(ori[1].toDouble()).toFloat()
        roll = Math.toDegrees(ori[2].toDouble()).toFloat()
        // circular smoothing so 359 -> 1 degrees does not jump
        if (!seeded) {
            sx = sin(az); sy = cos(az); seeded = true
        } else {
            sx += 0.25 * (sin(az) - sx)
            sy += 0.25 * (cos(az) - sy)
        }
        heading = Geo.norm360(Math.toDegrees(atan2(sx, sy))).toFloat()
    }

    private fun slowTick() {
        val now = System.currentTimeMillis()
        if (now - lastSlow < 100) return
        lastSlow = now
        sHeading = heading
        sPitch = pitch
        sRoll = roll
        sField = fieldF
        sgx = gx; sgy = gy; sgz = gz
    }

    fun start(ctx: Context) {
        stop()
        val m = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sm = m
        wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rv = m.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val ac = m.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val mg = m.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        haveRotVec = rv != null
        hasCompass = mg != null
        seeded = false; gInit = false; haveAcc = false; haveMag = false; fieldF = 0f
        if (rv != null) m.registerListener(listener, rv, SensorManager.SENSOR_DELAY_GAME)
        if (ac != null) m.registerListener(listener, ac, SensorManager.SENSOR_DELAY_GAME)
        if (mg != null) m.registerListener(listener, mg, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sm?.unregisterListener(listener)
        sm = null
    }
}

/** Last known position plus magnetic declination for that position. */
object Loc {
    var location by mutableStateOf<Location?>(null)
    var declination by mutableFloatStateOf(0f)
    private var lm: LocationManager? = null

    private val listener = object : LocationListener {
        override fun onLocationChanged(l: Location) { accept(l) }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun accept(l: Location) {
        val cur = location
        if (cur == null || l.provider == cur.provider || l.accuracy < cur.accuracy || l.time - cur.time > 30000) {
            location = l
            declination = try {
                GeomagneticField(l.latitude.toFloat(), l.longitude.toFloat(), l.altitude.toFloat(), System.currentTimeMillis()).declination
            } catch (e: Exception) { 0f }
        }
    }

    @Suppress("MissingPermission")
    fun start(ctx: Context) {
        stop()
        val m = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        lm = m
        for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)) {
            try { m.getLastKnownLocation(p)?.let { accept(it) } } catch (e: Exception) { }
            if (p != LocationManager.PASSIVE_PROVIDER) {
                try { m.requestLocationUpdates(p, 2000L, 1f, listener) } catch (e: Exception) { }
            }
        }
    }

    fun stop() {
        try { lm?.removeUpdates(listener) } catch (e: Exception) { }
        lm = null
    }
}

fun buzz(ctx: Context) {
    try {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        v?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
    } catch (e: Exception) { }
}

package com.mux.compass

import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Pure navigation math (no Android classes). */
object Geo {
    const val KAABA_LAT = 21.422487
    const val KAABA_LON = 39.826206

    private val POINTS = listOf(
        "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
    )

    fun norm360(d: Double): Double = ((d % 360.0) + 360.0) % 360.0

    /** Signed shortest angle a - b in the range -180..180 (positive = a is clockwise of b). */
    fun diff(a: Double, b: Double): Double {
        var d = norm360(a - b)
        if (d > 180.0) d -= 360.0
        return d
    }

    fun cardinal(h: Double): String = POINTS[((norm360(h) + 11.25) / 22.5).toInt() % 16]

    /** Initial great-circle bearing from point 1 to point 2, degrees clockwise from true north. */
    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dl = Math.toRadians(lon2 - lon1)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return norm360(Math.toDegrees(atan2(y, x)))
    }

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0088
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dp = p2 - p1
        val dl = Math.toRadians(lon2 - lon1)
        val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    fun qiblaBearing(lat: Double, lon: Double) = bearing(lat, lon, KAABA_LAT, KAABA_LON)
    fun qiblaDistanceKm(lat: Double, lon: Double) = distanceKm(lat, lon, KAABA_LAT, KAABA_LON)

    fun dms(v: Double, pos: String, neg: String): String {
        val a = abs(v)
        val d = a.toInt()
        val m = ((a - d) * 60).toInt()
        val s = (a - d - m / 60.0) * 3600.0
        return String.format(Locale.US, "%d°%02d'%04.1f\" %s", d, m, s, if (v >= 0) pos else neg)
    }

    /** Spirit level tilt angles (degrees) from a gravity/accelerometer vector. */
    fun tiltX(ax: Float, az: Float): Float = Math.toDegrees(atan2(ax.toDouble(), az.toDouble())).toFloat()
    fun tiltY(ay: Float, az: Float): Float = Math.toDegrees(atan2(ay.toDouble(), az.toDouble())).toFloat()
    fun edgeTilt(ax: Float, ay: Float, az: Float): Float =
        Math.toDegrees(atan2(ax.toDouble(), sqrt((ay * ay + az * az).toDouble()))).toFloat()
}

package io.github.sdsd08013.viewmarkerkit.compose

import android.graphics.Point
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.LatLng

/** Converts between map coordinates and overlay pixels. Wraps a map [Projection]; tests supply their own. */
internal interface ScreenProjection {
    fun toScreen(location: LatLng): ScreenPoint

    fun fromScreen(point: ScreenPoint): LatLng
}

internal fun Projection.asScreenProjection(): ScreenProjection = object : ScreenProjection {
    override fun toScreen(location: LatLng): ScreenPoint {
        val point = toScreenLocation(location)
        return ScreenPoint(point.x, point.y)
    }

    override fun fromScreen(point: ScreenPoint): LatLng = fromScreenLocation(Point(point.x, point.y))
}

/** Size of the area markers are placed in, in pixels. */
internal data class Viewport(val width: Int, val height: Int)

/** Camera state at one moment. */
internal data class CameraSnapshot(
    val projection: ScreenProjection,
    val zoom: Float,
    val bearing: Float,
    val center: LatLng,
)

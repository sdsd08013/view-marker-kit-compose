package io.github.sdsd08013.viewmarkerkit.compose.sample

import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.LatLng
import io.github.sdsd08013.viewmarkerkit.compose.MapMarker

/** A marker with a label. Only id, location and size are required by the library. */
data class Pin(
    override val id: Long,
    override var location: LatLng,
    val label: String,
    val color: Color,
) : MapMarker {
    override val sizeInDp: Int = 56
}

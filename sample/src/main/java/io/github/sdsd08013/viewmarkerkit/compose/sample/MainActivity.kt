package io.github.sdsd08013.viewmarkerkit.compose.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import io.github.sdsd08013.viewmarkerkit.compose.EdgeMode
import io.github.sdsd08013.viewmarkerkit.compose.MarkerOverlay

class MainActivity : ComponentActivity() {
    private var frameMetrics: FrameMetricsLogger? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // `adb shell am start ... --ei markers 200 --ez walk false` for stress tests
        val count = intent.getIntExtra("markers", 0)
        val pins = if (count <= 0) defaultPins else gridPins(count)
        val walk = intent.getBooleanExtra("walk", true)
        if (intent.getBooleanExtra("metrics", false)) frameMetrics = FrameMetricsLogger(this)
        setContent {
            MaterialTheme {
                MapScreen(pins, walk, metrics = intent.getBooleanExtra("metrics", false))
            }
        }
    }
}

private val defaultPins = listOf(
    Pin(1, LatLng(35.6812, 139.7671), "Tokyo", Color(0xFFE53935)),
    Pin(2, LatLng(35.6586, 139.7454), "Tower", Color(0xFF1E88E5)),
    Pin(3, LatLng(35.7101, 139.8107), "Skytree", Color(0xFF43A047)),
    Pin(4, LatLng(35.6938, 139.7034), "Shinjuku", Color(0xFFFB8C00)),
    Pin(5, LatLng(35.6595, 139.7005), "Shibuya", Color(0xFF8E24AA)),
)

/** [count] pins on a grid around Tokyo, about 0.6 degree wide. */
private fun gridPins(count: Int): List<Pin> {
    val columns = Math.ceil(Math.sqrt(count.toDouble())).toInt()
    val step = 0.6 / columns
    return List(count) { i ->
        val row = i / columns
        val col = i % columns
        Pin(
            id = (i + 1).toLong(),
            location = LatLng(35.68 - 0.3 + row * step, 139.75 - 0.3 + col * step),
            label = (i + 1).toString(),
            color = Color(0xFF000000 or ((0x4F7F9FL + i * 0x3A2B1CL) and 0xFFFFFFL)),
        )
    }
}

@Composable
private fun MapScreen(initialPins: List<Pin>, walk: Boolean, metrics: Boolean = false) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(35.68, 139.75), 12f)
    }
    // Counts camera updates (= overlay relayouts) and logs the rate (stress tests only)
    LaunchedEffect(metrics) {
        if (!metrics) return@LaunchedEffect
        var updates = 0L
        var windowStart = 0L
        snapshotFlow { cameraPositionState.position }.collect {
            val now = android.os.SystemClock.uptimeMillis()
            if (windowStart == 0L) windowStart = now
            updates++
            if (now - windowStart >= 2000) {
                android.util.Log.i("UpdateRate", "%.1f updates/s per marker".format(updates / ((now - windowStart) / 1000.0)))
                updates = 0; windowStart = now
            }
        }
    }

    var clamp by remember { mutableStateOf(false) }
    var pins by remember { mutableStateOf(initialPins) }

    // Move the first pin around in a small circle: a new list with a moved pin on every frame
    LaunchedEffect(walk) {
        if (!walk) return@LaunchedEffect
        val center = initialPins[0].location
        val start = withFrameMillis { it }
        while (true) {
            val angle = withFrameMillis { (it - start) % 20_000 / 20_000.0 * 2 * Math.PI }
            val moved = initialPins[0].copy(location = LatLng(center.latitude + 0.01 * Math.sin(angle), center.longitude + 0.01 * Math.cos(angle)))
            pins = listOf(moved) + initialPins.drop(1)
        }
    }

    val edgeMode = if (clamp) EdgeMode.Clamp(marginPx = with(LocalDensity.current) { 40.dp.roundToPx() }) else EdgeMode.None

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
        )

        MarkerOverlay(
            markers = pins,
            cameraPositionState = cameraPositionState,
            modifier = Modifier.fillMaxSize(),
            edgeMode = edgeMode,
        ) { pin, placement ->
            PinContent(pin, placement)
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shadowElevation = 4.dp,
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Clamp to edge", Modifier.padding(end = 8.dp))
                Switch(checked = clamp, onCheckedChange = { clamp = it })
            }
        }
    }
}

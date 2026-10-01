package io.github.sdsd08013.viewmarkerkit.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Where a marker currently is on screen. Read [descriptor] inside the marker content to react
 * to its placement (for example to the screen edge it is clamped to); reading it makes that
 * content recompose when the position changes.
 */
@Stable
class MarkerPlacement internal constructor() {
    var descriptor: MarkerPositionDescriptor? by mutableStateOf(null)
        internal set
}

/**
 * Places [content] for each of [markers] on top of a `GoogleMap`, synced with its camera.
 *
 * Put it after the map in the same `Box` with the same size. Pointer events on empty areas
 * fall through to the map; marker content handles its own clicks.
 *
 * Markers are matched by [MapMarker.identity]. Pass a new list (or new marker instances) when
 * markers are added, removed or move; positions follow the camera on their own.
 *
 * @param cameraPositionState the state of the map the overlay sits on
 * @param edgeMode how markers outside the viewport are handled
 */
@Composable
fun <M : MapMarker> MarkerOverlay(
    markers: List<M>,
    cameraPositionState: CameraPositionState,
    modifier: Modifier = Modifier,
    edgeMode: EdgeMode = EdgeMode.None,
    content: @Composable (marker: M, placement: MarkerPlacement) -> Unit,
) {
    val density = LocalDensity.current.density
    var viewport by remember { mutableStateOf(Viewport(0, 0)) }
    val engine = remember(edgeMode, density) {
        MarkerPositionEngine<M>(density = density, viewport = { viewport }, edgeMode = edgeMode)
    }
    val placements = remember(engine) { mutableStateMapOf<MarkerIdentity, MarkerPlacement>() }
    val tracked = remember(engine) { mutableMapOf<MarkerIdentity, TrackedMarker<M>>() }

    fun publish(descriptors: List<MarkerPositionDescriptor>) {
        for (descriptor in descriptors) {
            placements[descriptor.identifier]?.descriptor = descriptor
        }
    }

    // Attach, detach and move markers as the list changes
    LaunchedEffect(engine, markers, viewport) {
        val projection = cameraPositionState.projection?.asScreenProjection() ?: return@LaunchedEffect
        val current = markers.associateBy { it.identity }

        for (identity in tracked.keys - current.keys) {
            tracked.remove(identity)
            placements.remove(identity)
            engine.detach(identity)
        }
        for ((identity, marker) in current) {
            val previous = tracked[identity]
            when {
                previous == null -> {
                    engine.attach(marker, engine.descriptorFor(marker, projection))
                    placements[identity] = MarkerPlacement()
                }
                previous.marker !== marker || previous.location != marker.location -> {
                    engine.updateLocation(marker, projection, cameraPositionState.position.target)
                }
            }
            tracked[identity] = TrackedMarker(marker, marker.location)
        }

        engine.resetReferenceFrame()
        publish(engine.recalculate(cameraPositionState.snapshot(projection)))
    }

    // Follow the camera
    LaunchedEffect(engine, cameraPositionState) {
        snapshotFlow { cameraPositionState.position to cameraPositionState.isMoving }
            .distinctUntilChanged()
            .collect { (_, isMoving) ->
                val projection = cameraPositionState.projection?.asScreenProjection() ?: return@collect
                publish(engine.recalculate(cameraPositionState.snapshot(projection)))
                if (!isMoving) engine.resetReferenceFrame()
            }
    }

    Layout(
        modifier = modifier.onSizeChanged { viewport = Viewport(it.width, it.height) },
        content = {
            for (marker in markers) {
                key(marker.identity) {
                    content(marker, placements[marker.identity] ?: remember { MarkerPlacement() })
                }
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.mapIndexed { index, measurable ->
            val size = markers[index].sizeInPx(density)
            measurable.measure(Constraints.fixed(size, size))
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                // Read in the layout phase so camera moves relayout without recomposing
                val position = placements[markers[index].identity]?.descriptor?.screenPosition ?: return@forEachIndexed
                placeable.place(position.x, position.y)
            }
        }
    }
}

private class TrackedMarker<M : MapMarker>(val marker: M, val location: LatLng)

private fun CameraPositionState.snapshot(projection: ScreenProjection) = CameraSnapshot(
    projection = projection,
    zoom = position.zoom,
    bearing = position.bearing,
    center = position.target,
)

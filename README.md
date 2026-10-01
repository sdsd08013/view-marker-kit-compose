# ViewMarkerKit Compose

[![CI](https://github.com/sdsd08013/view-marker-kit-compose/actions/workflows/ci.yml/badge.svg)](https://github.com/sdsd08013/view-marker-kit-compose/actions/workflows/ci.yml)

Render composables as markers on Google Maps, on top of [maps-compose](https://github.com/googlemaps/android-maps-compose).

`MarkerComposable` in maps-compose rasterizes a composable into a marker image. ViewMarkerKit Compose
keeps the composable itself on top of the map and moves it with the camera, so the marker can run
animations and have individually tappable parts.

The library only places composables and follows the camera. Which markers to show (clustering,
decluttering, focus, ...) is up to you.

- Composable markers synced with the camera (delta calculation while panning, relayout without recomposition)
- Off-screen markers clamped to the screen edge (`EdgeMode.Clamp`)
- Pointer events on empty areas fall through to the map

This is the Compose counterpart of [view-marker-kit](https://github.com/sdsd08013/view-marker-kit),
which does the same for Android views. The two libraries are independent.

> Work in progress. The API is not stable yet.

## Setup

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// build.gradle.kts
dependencies {
    implementation("com.github.sdsd08013:view-marker-kit-compose:<tag>")
}
```

## Usage

```kotlin
// 1. Implement the marker contract: id, location and the size in dp
data class Pin(override val id: Long, override var location: LatLng) : MapMarker {
    override val sizeInDp = 48
}

// 2. Put MarkerOverlay after the map, with the same size, and give it the map's camera state
val cameraPositionState = rememberCameraPositionState()

Box(Modifier.fillMaxSize()) {
    GoogleMap(Modifier.fillMaxSize(), cameraPositionState = cameraPositionState)

    MarkerOverlay(
        markers = pins,
        cameraPositionState = cameraPositionState,
        modifier = Modifier.fillMaxSize(),
    ) { pin, placement ->
        PinContent(pin)   // any composable; it is measured at pin.sizeInDp
    }
}
```

Markers are matched by `MapMarker.identity` (defaults to `id`). To add, remove or move markers,
pass a new list; to animate a move, emit a new list with the moved marker on each frame.

`placement.descriptor` tells the content where it is, for example `isAtEdge` with
`EdgeMode.Clamp`. Reading it makes that content recompose when the position changes; content
that does not read it is only relaid out.

## Public API

| Type | Role |
|---|---|
| `MapMarker` / `MarkerIdentity` | Marker contract: location, size and identity (defaults to `id`) |
| `MarkerOverlay` / `MarkerPlacement` | The overlay composable and the per-marker placement it hands to content |
| `EdgeMode` / `MarkerEdge` | How off-screen markers are handled: keep as is, or clamp to the edge |
| `MarkerPositionDescriptor` / `ScreenPoint` | Placement info |

## Sample

[`sample`](sample) shows pins over Tokyo, one of them moving, tap to bounce, and a switch for
`EdgeMode.Clamp`. To run it, put a Google Maps API key in `local.properties` (it is read into the
manifest at build time and never committed):

```
MAPS_API_KEY=your_key
```

## Requirements

- minSdk 26 / compileSdk 37
- Dependencies: maps-compose, Compose foundation, play-services-maps

## License

Apache License 2.0. See [LICENSE](LICENSE).

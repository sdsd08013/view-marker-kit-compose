package io.github.sdsd08013.viewmarkerkit.compose

/**
 * Key that identifies a marker. Implementations must have value-based `equals` / `hashCode`.
 *
 * When [MapMarker.id] alone is unique, the default [IdMarkerIdentity] is used and
 * no implementation is needed.
 */
interface MarkerIdentity

/** [MarkerIdentity] backed by [MapMarker.id]. The default for [MapMarker.identity]. */
data class IdMarkerIdentity(val id: Long) : MarkerIdentity

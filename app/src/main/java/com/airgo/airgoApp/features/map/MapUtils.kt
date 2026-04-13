package com.airgo.airgoApp.features.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.LayerDrawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.airgo.airgoApp.R
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/**
 * Aquest fitxer conté funcions i utilitats de lògica del mapa (càlculs, configuracions, etc)
 */

private const val CACHE_CAPACITY = 120

// Límits del mapa
private const val LAT_NORTH = 41.55
private const val LAT_SOUTH = 41.2
private const val LNG_WEST = 2.0
private const val LNG_EAST = 2.3
private const val MIN_ZOOM = 12.0
private const val MAX_ZOOM = 19.0

/**
 * Configuració de la vista del mapa
 */
internal fun configureMapView(mapView: MapView) {
    // Utilitzar més cache per evitar veure el mapa pixelat
    val cache = mapView.tileProvider.tileCache
    cache?.ensureCapacity(CACHE_CAPACITY)

    // Optimització hardware
    mapView.isTilesScaledToDpi = true
    mapView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

    // Limitar el mapa a Barcelona
    mapView.minZoomLevel = MIN_ZOOM
    mapView.maxZoomLevel = MAX_ZOOM
    mapView.setScrollableAreaLimitLatitude(LAT_NORTH, LAT_SOUTH, 0)
    mapView.setScrollableAreaLimitLongitude(LNG_WEST, LNG_EAST, 0)
}

/**
 * Funció que gestiona els taps al mapa. Si s'està en mode de selecció, envia el punt seleccionat al ViewModel. Si no,
 * deselecciona qualsevol marcador seleccionat.
 */
internal fun createMapEventsOverlay(
    mapView: MapView,
    bicingOverlay: FolderOverlay,
    selectionModeProvider: () -> MapSelectionMode,
    onPointSelectedProvider: () -> (GeoPoint) -> Unit,
): MapEventsOverlay {
    val receiver =
        object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p == null) return true

                val currentMode = selectionModeProvider()
                if (currentMode != MapSelectionMode.NONE) {
                    onPointSelectedProvider()(p)
                } else {
                    resetBicingMarkers(bicingOverlay, mapView)
                }

                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }
    return MapEventsOverlay(receiver)
}

/**
 * Funció auxiliar per deseleccionar marcadors i tancar finestres.
 */
private fun resetBicingMarkers(
    bicingOverlay: FolderOverlay,
    mapView: MapView,
) {
    bicingOverlay.items.filterIsInstance<Marker>().forEach { marker ->
        marker.closeInfoWindow()
        if (marker is BicingHelper.BicingMarker) {
            marker.setSelected(false, mapView)
        }
    }
}

/**
 * Funció per crear un marcador de ruta (inici o destí) amb un color específic.
 */
private fun createRouteMarker(
    map: MapView,
    point: GeoPoint,
    color: Int,
): Marker {
    val marker =
        Marker(map).apply {
            position = point
            title = "RouteMarker"
            icon = ContextCompat.getDrawable(map.context, R.drawable.ic_route_marker)?.mutate()
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
    (marker.icon as? LayerDrawable)?.let { layer ->
        val flag = layer.findDrawableByLayerId(R.id.flag_layer)
        flag?.let { DrawableCompat.setTint(DrawableCompat.wrap(it).mutate(), color) }
    }
    return marker
}

/**
 * Funció per dibuixar els pins d'inici i destí i la ruta al mapa.
 * Neteja les capes de ruta anteriors abans de pintar les noves.
 */
internal fun updateRouteElements(
    map: MapView,
    state: MapState,
) {
    // Netejar els elements de ruta anteriors (marcadors i línies)
    val toRemove =
        map.overlays.filter { overlay ->
            (overlay is Marker && overlay.title == "RouteMarker") ||
                (overlay is FolderOverlay && (overlay.name == "RouteLayer" || overlay.name == "RouteElement"))
        }
    map.overlays.removeAll(toRemove)

    // Pintar la ruta si existeix
    state.currentRoute?.let { route ->
        val routeOverlay = RouteHelper.createRouteOverlay(map, route)
        routeOverlay.name = "RouteElement"
        map.overlays.add(1, routeOverlay)
    }

    // Pintar els marcadors d'inici i destí
    state.startPoint?.let {
        map.overlays.add(createRouteMarker(map, it, android.graphics.Color.parseColor("#4A7C59")))
    }
    state.endPoint?.let {
        map.overlays.add(createRouteMarker(map, it, android.graphics.Color.RED))
    }

    map.invalidate()
}

/**
 * Funció per comprovar si el GPS està activat al dispositiu.
 * Retorna true si el GPS està habilitat, false en cas contrari.
 */
internal fun isGpsEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
}

/**
 * Comprovar si l'aplicació té permissos de localització
 */
internal fun checkInitialLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

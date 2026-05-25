package com.airgo.airgoApp.features.map

import android.graphics.Color
import android.graphics.Color.RED
import android.graphics.drawable.LayerDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.DrawableCompat
import com.airgo.airgoApp.R
import com.airgo.airgoApp.features.map.data.FavoriteRouteDetail
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private const val BCN_LAT_NORTH = 41.50
private const val BCN_LAT_SOUTH = 41.25
private const val BCN_LON_WEST = 2.00
private const val BCN_LON_EAST = 2.30
private const val MIN_ZOOM = 12.0
private const val MAX_ZOOM = 19.0
private const val BOUNDING_BOX_SCALE = 1.3f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun routePreviewScreen(
    route: FavoriteRouteDetail,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        route.startAddress ?: route.startLocation
                            ?: stringResource(R.string.favorite_routes_screen_title),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            routeMapView(route = route)
        }
    }
}

private const val LINE_WIDTH = 8f

@Composable
private fun routeMapView(route: FavoriteRouteDetail) {
    val points =
        remember(route.routePoints) {
            route.routePoints.mapNotNull { pair ->
                if (pair.size >= 2) GeoPoint(pair[1], pair[0]) else null
            }
        }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = ctx.packageName
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isTilesScaledToDpi = true
                setScrollableAreaLimitLatitude(BCN_LAT_NORTH, BCN_LAT_SOUTH, 0)
                setScrollableAreaLimitLongitude(BCN_LON_WEST, BCN_LON_EAST, 0)
                minZoomLevel = MIN_ZOOM
                maxZoomLevel = MAX_ZOOM

                if (points.isNotEmpty()) {
                    val polyline =
                        Polyline().apply {
                            setPoints(points)
                            outlinePaint.color = android.graphics.Color.parseColor("#4FC3F7")
                            outlinePaint.strokeWidth = LINE_WIDTH
                        }
                    overlays.add(polyline)

                    // Pin inici (verd)
                    val startMarker =
                        Marker(this).apply {
                            position = points.first()
                            title = "Inici"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = ctx.getDrawable(R.drawable.ic_route_marker)?.mutate()
                        }
                    (startMarker.icon as? LayerDrawable)?.let { layer ->
                        val flag = layer.findDrawableByLayerId(R.id.flag_layer)
                        flag?.let {
                            DrawableCompat.setTint(DrawableCompat.wrap(it).mutate(), Color.parseColor("#4A7C59"))
                        }
                    }
                    overlays.add(startMarker)

                    // Pin destí (vermell)
                    val endMarker =
                        Marker(this).apply {
                            position = points.last()
                            title = "Destí"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = ctx.getDrawable(R.drawable.ic_route_marker)?.mutate()
                        }
                    (endMarker.icon as? LayerDrawable)?.let { layer ->
                        val flag = layer.findDrawableByLayerId(R.id.flag_layer)
                        flag?.let { DrawableCompat.setTint(DrawableCompat.wrap(it).mutate(), RED) }
                    }
                    overlays.add(endMarker)

                    if (points.size >= 2) {
                        val boundingBox = BoundingBox.fromGeoPoints(points)
                        post { zoomToBoundingBox(boundingBox.increaseByScale(BOUNDING_BOX_SCALE), false) }
                    } else {
                        controller.setCenter(points.first())
                    }
                }
            }
        },
        update = {},
    )
}

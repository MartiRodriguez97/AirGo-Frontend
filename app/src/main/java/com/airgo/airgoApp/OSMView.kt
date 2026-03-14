package com.airgo.airgoApp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private const val BCN_LATITUDE = 41.3851 // Latitud inicial
private const val BCN_LONGITUDE = 2.1734 // Longitud inicial
private const val INITIAL_ZOOM_LEVEL = 15.0 // Zoom inicial
private const val BLUE_DOT_SIZE = 40 // Mida del punt
private const val BLUE_DOT_COLOR = 0xFF4285F4.toInt() // Color del punt
private const val WHITE_BORDER_COLOR = 0xFFFFFFFF.toInt() // Vora del punt

private const val INNER_CIRCLE_RATIO = 2.5f

@Composable
fun osmView(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Conectar-se a OMS a través d'un user agent
    Configuration.getInstance().userAgentValue = context.packageName

    // MapView
    val mapView = remember { MapView(context) }

    // Capa d'ubicació
    val locationOverlay =
        remember {
            val provider = GpsMyLocationProvider(context)
            MyLocationNewOverlay(provider, mapView)
        }

    // Gestió de permissos
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    // Mirar si ja tenia permissos
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        }

    // Si no tenia permissos, demanar-los
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            launcher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    // Parar el gps i osm quan surts de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            locationOverlay.disableMyLocation()
            mapView.onPause()
        }
    }

    // Pont entre OSM i Jetpack Compose
    AndroidView(
        modifier = modifier,
        factory = {
            setupMapView(mapView, locationOverlay)
            mapView
        },
        update = { map ->
            map.onResume()
            if (hasLocationPermission) {
                if (!locationOverlay.isMyLocationEnabled) {
                    locationOverlay.enableMyLocation()
                    locationOverlay.enableFollowLocation()
                }
            }
        },
    )
}

/**
 * Funció auxiliar per pintar i configurar el mapa. Centra el mapa en la ubicació obtinguda
 * (o en un punt definit si no s'han donat els permissos), zoom
 */
private fun setupMapView(
    mapView: MapView,
    locationOverlay: MyLocationNewOverlay,
) {
    mapView.apply {
        setTileSource(TileSourceFactory.MAPNIK) // Estil del mapa per defecte
        setMultiTouchControls(true) // Zoom amb dos dits
        controller.setZoom(INITIAL_ZOOM_LEVEL) // Zoom incial

        // Centrar inicialment a un punt definit
        controller.setCenter(GeoPoint(BCN_LATITUDE, BCN_LONGITUDE))

        // Configuració de l'estil del punt de la ubicació
        val dot = createDotBitmap()
        locationOverlay.setPersonIcon(dot) // Icona quan estàs aturat
        locationOverlay.setDirectionIcon(dot) // Icona quan t'estàs movent
        locationOverlay.setPersonHotspot(BLUE_DOT_SIZE / 2f, BLUE_DOT_SIZE / 2f)

        // Desplaçar el mapa a la teva ubicació (si s'han donat permnissos)
        locationOverlay.runOnFirstFix {
            val myLocation = locationOverlay.myLocation
            if (myLocation != null) {
                post {
                    controller.animateTo(myLocation)
                    controller.setZoom(INITIAL_ZOOM_LEVEL)
                }
            }
        }

        // Pintar la capa d'ubicació a sobre el mapa
        overlays.add(locationOverlay)
    }
}

/**
 * Funció auxiliar per dibuixar el punt d'ubicació
 */
private fun createDotBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(BLUE_DOT_SIZE, BLUE_DOT_SIZE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply { isAntiAlias = true }

    // Dibuixar vora
    paint.color = WHITE_BORDER_COLOR
    canvas.drawCircle(BLUE_DOT_SIZE / 2f, BLUE_DOT_SIZE / 2f, BLUE_DOT_SIZE / 2f, paint)

    // Dibuixar el punt
    paint.color = BLUE_DOT_COLOR
    canvas.drawCircle(BLUE_DOT_SIZE / 2f, BLUE_DOT_SIZE / 2f, BLUE_DOT_SIZE / INNER_CIRCLE_RATIO, paint)

    return bitmap
}

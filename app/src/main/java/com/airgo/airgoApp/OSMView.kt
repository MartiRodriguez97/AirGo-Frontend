package com.airgo.airgoApp

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private const val BCN_LATITUDE = 41.3851
private const val BCN_LONGITUDE = 2.1734
private const val INITIAL_ZOOM_LEVEL = 15.0

@Composable
fun osmView(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    //Conectar-se a OMS a través d'un user agent
    Configuration.getInstance().userAgentValue = context.packageName

    /*
    //Demanar permissos
    val locationPermissionRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        //Gestionar si no s'accepten els permissos
    }
    LaunchedEffect(Unit) {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,       //Demanar ubi precisa
                Manifest.permission.ACCESS_COARSE_LOCATION      //Demanar ubi aproximada
            )
        )
    }
     */

    //Pont entre OSM i Jetpack Compose
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)         //Estil del mapa per defecte
                setMultiTouchControls(true)                     //Zoom amb dos dits
                controller.setZoom(INITIAL_ZOOM_LEVEL)                        //Zoom incial
                val barcelona = GeoPoint(BCN_LATITUDE, BCN_LONGITUDE)
                controller.setCenter(barcelona)                 //Centrar el mapa a Barcelona inicialment

                /*
                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                locationOverlay.enableMyLocation()              //Mostra el punt de la teva ubi
                locationOverlay.enableFollowLocation()          //La càmera et segueix si et mous
                this.overlays.add(locationOverlay)              //Afegir la capa a sobre el mapa
                 */
            }
        },
        update = { map ->
            // El mapa s'actualitzarà automàticament gràcies a l'overlay
        }
    )
}

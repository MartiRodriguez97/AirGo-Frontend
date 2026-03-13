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

@Composable
fun OSMView(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    //Conectar-se a OMS a través d'un user agent
    Configuration.getInstance().userAgentValue = context.packageName

    //Pont entre OSM i Jetpack Compose
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)         //Estil del mapa per defecte
                setMultiTouchControls(true)                     //Zoom amb dos dits
                controller.setZoom(15.0)                        //Zoom incial
                val barcelona = GeoPoint(41.3851, 2.1734)
                controller.setCenter(barcelona)                 //Centrar el mapa a Barcelona inicialment
            }
        },
        update = { map ->
            // El mapa s'actualitzarà automàticament gràcies a l'overlay
        }
    )
}
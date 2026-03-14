package com.airgo.airgoApp

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

private const val BCN_LATITUDE = 41.3851 // Latitud inicial
private const val BCN_LONGITUDE = 2.1734 // Longitud inicial
private const val INITIAL_ZOOM_LEVEL = 15.0 // Zoom inicial

@Composable
fun osmView(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Conectar-se a OMS a través d'un user agent
    Configuration.getInstance().userAgentValue = context.packageName

    // Variable per saber si s'activa/desactiva la capa de qualitat de l'aire
    var showPollution by remember { mutableStateOf(true) }

    //Menú desplegable obert/tancat
    var expanded by remember { mutableStateOf(false) }

    // MapView
    val mapView = remember { MapView(context) }

    // Capa d'ubicació. Crida a LocationHelper que retorna la capa (overlay) amb la localització
    val locationOverlay = remember { LocationHelper.createLocationOverlay(context, mapView) }

    // Capa de contaminació
    val pollutionOverlay = remember { PollutionHelper.createPollutionOverlay(mapView) }

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

    // Descativar capa de localització i mapa quan se surt de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            locationOverlay.disableMyLocation()
            // ################# Desactivar capa de presentació quan n'hi hagi ###############################
            mapView.onPause()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Pont entre OSM i Jetpack Compose
        AndroidView(
            modifier = modifier,
            factory = {
                mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK) // Estil del mapa per defecte
                    setMultiTouchControls(true) // Zoom amb dos dits
                    controller.setZoom(INITIAL_ZOOM_LEVEL) // Zoom incial

                    // Centrar inicialment a un punt definit
                    controller.setCenter(GeoPoint(BCN_LATITUDE, BCN_LONGITUDE))

                    // Pintar les capes (La última es mostra a sobre)
                    overlays.add(pollutionOverlay)
                    overlays.add(locationOverlay)
                }
            },
            update = { map ->
                map.onResume()
                // Refrescar la ubicació
                if (hasLocationPermission && !locationOverlay.isMyLocationEnabled) {
                    locationOverlay.enableMyLocation()
                    locationOverlay.enableFollowLocation()
                }
                // Capa de contaminació activa/desactiva
                pollutionOverlay.isEnabled = showPollution
            },
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp) //
        ) {
            FloatingActionButton(
                onClick = { expanded = true },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Canviar capes")
            }

            // MENÚ DESPLEGABLE
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Qualitat de l'aire") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Cloud, // O Icons.Default.Warning
                            contentDescription = null,
                            tint = if (showPollution) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    },
                    onClick = {
                        showPollution = !showPollution
                        expanded = false
                    }
                )
            }
        }
    }
}

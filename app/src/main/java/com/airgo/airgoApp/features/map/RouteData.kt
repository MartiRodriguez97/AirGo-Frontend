package com.airgo.airgoApp.features.map

/**
 * Aquest fitxer defineix les dades relacionades amb les rutes, incloent les sol·licituds i respostes de l'API.
 */

data class RouteRequest(
    @com.google.gson.annotations.SerializedName("lat_start")
    val latStart: Double,
    @com.google.gson.annotations.SerializedName("lon_start")
    val lonStart: Double,
    @com.google.gson.annotations.SerializedName("lat_end")
    val latEnd: Double,
    @com.google.gson.annotations.SerializedName("lon_end")
    val lonEnd: Double,
)

/**
 * La classe RouteResponse representa la resposta de l'API de rutes,
 * que inclou la geometria de la ruta, un resum de la ruta i els detalls de contaminació al llarg del camí.
 */

data class RouteResponse(
    val geometry: RouteGeometry,
    val summary: RouteSummary,
    val pollutionDetails: List<List<Double>>, // [[inicio, fin, valor], ...]
)

/**
 * La classe RouteGeometry representa la geometria de la ruta, que és una llista de coordenades
 * (latitud i longitud) que defineixen el camí a seguir.
 */
data class RouteGeometry(
    val coordinates: List<List<Double>>, // [[lat, lon], [lat, lon], ...]
)

/**
 * La classe RouteSummary proporciona un resum de la ruta, incloent la durada estimada en minuts
 * i la distància total en metres.
 */

data class RouteSummary(
    @com.google.gson.annotations.SerializedName("duration_minutes")
    val durationMinutes: Double,
    @com.google.gson.annotations.SerializedName("distance_meters")
    val distanceMeters: Int,
)

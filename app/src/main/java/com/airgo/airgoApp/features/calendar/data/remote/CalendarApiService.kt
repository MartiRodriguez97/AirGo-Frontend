package com.airgo.airgoApp.features.calendar.data.remote

import com.airgo.airgoApp.core.network.ApiEndpoints.PLA_ENTRENAMENT
import com.airgo.airgoApp.features.calendar.domain.model.PlaEntrenament
import retrofit2.Response
import retrofit2.http.GET

interface CalendarApiService {
    @GET(PLA_ENTRENAMENT)
    suspend fun getPlaEntrenament(): Response<List<PlaEntrenament>>
}

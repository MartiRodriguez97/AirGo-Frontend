package com.airgo.airgoApp.features.map

import com.airgo.airgoApp.core.network.ApiEndpoints
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FavoriteRouteApiService {
    @POST(ApiEndpoints.SAVE_FAVORITE_ROUTE)
    suspend fun saveFavoriteRoute(
        @Body request: SaveFavoriteRouteRequest,
    ): Response<Unit>

    @GET(ApiEndpoints.FAVORITE_ROUTES)
    suspend fun getFavoriteRoutes(
        @Query("search") search: String? = null,
    ): List<FavoriteRouteItem>

    @DELETE(ApiEndpoints.DELETE_FAVORITE_ROUTE)
    suspend fun deleteFavoriteRoute(
        @Path("route_id") routeId: Int,
    ): Response<Unit>
}

package com.airgo.airgoApp.features.social.forums.data

import com.airgo.airgoApp.core.network.ApiEndpoints.DELETE_FAVORIT_FORUM
import com.airgo.airgoApp.core.network.ApiEndpoints.FAVORIT_FORUMS
import com.airgo.airgoApp.core.network.ApiEndpoints.FORUMS
import com.airgo.airgoApp.core.network.ApiEndpoints.FORUM_DETAIL
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ForumApi {
    // Barra de cerca de fòrums a la pantalla de Descobrir (Get tots els fòrums)
    @GET(FORUMS)
    suspend fun getForums(
        @Query("search") search: String? = null,
    ): List<Forum>

    // Crear un nou fòrum (nom + descripció)
    @POST(FORUMS)
    suspend fun createForum(
        @Body request: CreateForumRequest,
    ): Forum

    // Eliminar un fòrum creat per l'usuari (només si el creador és l'usuari autenticat)
    @DELETE(FORUM_DETAIL)
    suspend fun deleteForum(
        @Path("id") forumId: Int,
    ): Response<Unit>

    // Obtenir la llista de fòrums favorits de l'usuari autenticat
    @GET(FAVORIT_FORUMS)
    suspend fun getFavoritForums(): List<FavoritForumResponse>

    // Afegir un fòrum a la llista de favorits de l'usuari autenticat
    @POST(FAVORIT_FORUMS)
    suspend fun addFavoritForum(
        @Body request: AddFavoritForumRequest,
    ): Response<Unit>

    // Eliminar un fòrum de la llista de favorits de l'usuari autenticat
    @DELETE(DELETE_FAVORIT_FORUM)
    suspend fun removeFavoritForum(
        @Path("forum_id") forumId: Int,
    ): Response<Unit>
}

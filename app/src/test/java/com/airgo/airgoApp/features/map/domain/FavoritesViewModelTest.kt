package com.airgo.airgoApp.features.map.domain

import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.utils.UiText
import com.airgo.airgoApp.features.map.data.FavoriteRouteApiService
import com.airgo.airgoApp.features.map.data.FavoriteRouteDetail
import com.airgo.airgoApp.features.map.data.FavoriteRouteItem
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var favoriteApi: FavoriteRouteApiService

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        favoriteApi = mockk()
        mockkObject(RetrofitProvider)
        every { RetrofitProvider.favoriteRouteApi } returns favoriteApi
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `fetchFavoriteRoutes success should update routes list`() =
        runTest {
            val routes =
                listOf(
                    FavoriteRouteItem(1, mockk(), "2023-10-27T10:00:00Z"),
                )
            coEvery { favoriteApi.getFavoriteRoutes(any()) } returns routes

            val viewModel = FavoritesViewModel()
            viewModel.fetchFavoriteRoutes()
            advanceUntilIdle()

            assertEquals(routes, viewModel.routes)
            assertNull(viewModel.errorMessage)
        }

    @Test
    fun `fetchFavoriteRoutes failure should set error message`() =
        runTest {
            coEvery { favoriteApi.getFavoriteRoutes(any()) } throws java.io.IOException()

            val viewModel = FavoritesViewModel()
            viewModel.fetchFavoriteRoutes()
            advanceUntilIdle()

            assertEquals(R.string.error_fetching_routes, (viewModel.errorMessage as UiText.StringResource).resId)
        }

    @Test
    fun `deleteFavoriteRoute success should remove route from list`() =
        runTest {
            val detail = mockk<FavoriteRouteDetail> { every { id } returns 1 }
            val item = FavoriteRouteItem(100, detail, "2023-10-27T10:00:00Z")

            coEvery { favoriteApi.getFavoriteRoutes(any()) } returns listOf(item)
            // Explicitly specify the type to avoid ambiguity and use 204 code
            coEvery { favoriteApi.deleteFavoriteRoute(1) } returns Response.success<Unit>(204, null)

            val viewModel = FavoritesViewModel()
            viewModel.fetchFavoriteRoutes()
            advanceUntilIdle()

            viewModel.deleteFavoriteRoute(1)
            advanceUntilIdle()

            assertEquals(0, viewModel.routes.size)
        }

    @Test
    fun `deleteFavoriteRoute failure should set error message`() =
        runTest {
            coEvery { favoriteApi.deleteFavoriteRoute(any()) } returns Response.error(400, mockk(relaxed = true))

            val viewModel = FavoritesViewModel()
            viewModel.deleteFavoriteRoute(1)
            advanceUntilIdle()

            assertEquals(R.string.error_deleting_favourite_route, (viewModel.errorMessage as UiText.StringResource).resId)
        }

    @Test
    fun `selectRoute and clearSelectedRoute should work correctly`() {
        val viewModel = FavoritesViewModel()
        val detail = mockk<FavoriteRouteDetail>()

        viewModel.selectRoute(detail)
        assertEquals(detail, viewModel.selectedRoute)

        viewModel.clearSelectedRoute()
        assertNull(viewModel.selectedRoute)
    }

    @Test
    fun `clearError should set errorMessage to null`() =
        runTest {
            coEvery { favoriteApi.getFavoriteRoutes(any()) } throws java.io.IOException()
            val viewModel = FavoritesViewModel()
            viewModel.fetchFavoriteRoutes()
            advanceUntilIdle()

            viewModel.clearError()
            assertNull(viewModel.errorMessage)
        }
}

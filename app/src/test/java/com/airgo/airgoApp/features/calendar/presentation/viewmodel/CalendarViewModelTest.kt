package com.airgo.airgoApp.features.calendar.presentation.viewmodel

import com.airgo.airgoApp.features.calendar.data.remote.CalendarApiService
import com.airgo.airgoApp.features.calendar.domain.model.DetallsEspecifics
import com.airgo.airgoApp.features.calendar.domain.model.Exercici
import com.airgo.airgoApp.features.calendar.domain.model.PlaEntrenament
import com.airgo.airgoApp.features.calendar.domain.model.Template
import com.airgo.airgoApp.features.calendar.domain.viewmodel.CalendarViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private lateinit var apiService: CalendarApiService
    private lateinit var viewModel: CalendarViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        apiService = mockk()
        viewModel = CalendarViewModel(apiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchPlaEntrenament should update list on success`() =
        runTest {
            val mockData =
                listOf(
                    PlaEntrenament(
                        id = 1,
                        diesDurada = 30,
                        numEntrenamentsSetmanals = 3,
                        templates =
                            listOf(
                                Template(
                                    nom = "Sessio de Bici",
                                    descripcio = "Ciclisme",
                                    tipusExercici = "BIC",
                                    exercicis =
                                        listOf(
                                            Exercici(
                                                dataInici = "2024-05-22T18:00:00Z",
                                                dataFi = null,
                                                completat = false,
                                                tipusExercici = "BIC",
                                                detallsEspecifics = DetallsEspecifics(distFetaKm = 12.0),
                                            ),
                                        ),
                                ),
                            ),
                    ),
                )

            coEvery { apiService.getPlaEntrenament() } returns Response.success(mockData)

            viewModel.fetchPlaEntrenament()

            assertFalse(viewModel.isLoading.value)
            assertNull(viewModel.errorResId.value)
            assertEquals(mockData, viewModel.plaEntrenamentList.value)
        }

    @Test
    fun `fetchPlaEntrenament should handle API error`() =
        runTest {
            coEvery { apiService.getPlaEntrenament() } returns Response.error(500, mockk(relaxed = true))

            viewModel.fetchPlaEntrenament()

            assertFalse(viewModel.isLoading.value)
            assertEquals(com.airgo.airgoApp.R.string.calendar_error_server, viewModel.errorResId.value)
            assertEquals(emptyList<PlaEntrenament>(), viewModel.plaEntrenamentList.value)
        }

    @Test
    fun `fetchPlaEntrenament should handle network error`() =
        runTest {
            coEvery { apiService.getPlaEntrenament() } throws java.io.IOException()

            viewModel.fetchPlaEntrenament()

            assertFalse(viewModel.isLoading.value)
            assertEquals(com.airgo.airgoApp.R.string.calendar_error_network, viewModel.errorResId.value)
        }

    @Test
    fun `fetchPlaEntrenament should handle null body`() =
        runTest {
            coEvery { apiService.getPlaEntrenament() } returns Response.success(null)

            viewModel.fetchPlaEntrenament()

            assertFalse(viewModel.isLoading.value)
            assertNull(viewModel.errorResId.value)
            assertEquals(emptyList<PlaEntrenament>(), viewModel.plaEntrenamentList.value)
        }

    @Test
    fun `clearError should set errorResId to null`() =
        runTest {
            coEvery { apiService.getPlaEntrenament() } returns Response.error(500, mockk(relaxed = true))
            viewModel.fetchPlaEntrenament()
            assertEquals(com.airgo.airgoApp.R.string.calendar_error_server, viewModel.errorResId.value)

            viewModel.clearError()

            assertNull(viewModel.errorResId.value)
        }
}

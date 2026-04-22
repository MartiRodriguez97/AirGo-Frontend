package com.airgo.airgoApp.features.settings.account.titols.domain

import com.airgo.airgoApp.core.network.ApiEndpoints
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.settings.account.titols.data.AvailableTitolResponse
import com.airgo.airgoApp.features.settings.account.titols.data.TitolApi
import com.airgo.airgoApp.features.settings.account.titols.data.UserTitol
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class TitolViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val mockTitolApi = mockk<TitolApi>()
    private lateinit var viewModel: TitolViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(SessionManager)
        mockkObject(RetrofitProvider)
        mockkObject(ApiEndpoints)

        every { RetrofitProvider.titolApi } returns mockTitolApi
        every { SessionManager.userId } returns 123
        every { ApiEndpoints.getAvailableTitolsEndpoint(any()) } returns "url_titols"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `cuando inicia, si hay titulos en cache, los carga sin llamar a la API`() =
        runTest {
            // Preparación: Ponemos algo en el caché del SessionManager
            val listaCache = listOf(UserTitol(1, "Explorador", "Desc"))
            every { SessionManager.availableTitols } returns listaCache

            viewModel = TitolViewModel()

            assertEquals(listaCache, viewModel.availableTitols.value)
            coVerify(exactly = 0) { mockTitolApi.getAvailableTitols(any()) }
        }

    @Test
    fun `cuando inicia, si no hay cache, llama a la API y actualiza la lista`() =
        runTest {
            // Preparación: Cache vacío
            every { SessionManager.availableTitols } returns null
            val titolApi = UserTitol(2, "Piloto", "Desc")
            val respuestaApi = listOf(AvailableTitolResponse(titolApi))

            coEvery { mockTitolApi.getAvailableTitols(any()) } returns Response.success(respuestaApi)
            every { SessionManager.availableTitols = any() } returns Unit

            // Ejecución
            viewModel = TitolViewModel()
            advanceUntilIdle()

            // Verificación
            assertEquals(1, viewModel.availableTitols.value.size)
            assertEquals("Piloto", viewModel.availableTitols.value[0].nom)
        }

    @Test
    fun `cuando cambiamos el titulo con exito, el estado es Success`() =
        runTest {
            every { SessionManager.availableTitols } returns emptyList()
            viewModel = TitolViewModel()

            // Mock de la petición de cambio
            every { ApiEndpoints.getChangeInfoEndpoint(123) } returns "url_change"
            coEvery { mockTitolApi.changeTitol(any(), any()) } returns Response.success(null)
            every { SessionManager.titol = any() } returns Unit

            viewModel.changeTitol("Nuevo Titulo")
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is TitolUiState.Success)
        }
}

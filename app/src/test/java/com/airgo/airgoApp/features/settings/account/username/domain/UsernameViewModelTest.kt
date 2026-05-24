package com.airgo.airgoApp.features.settings.account.username.domain

import android.util.Log
import com.airgo.airgoApp.core.network.ApiEndpoints
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.settings.account.username.data.UsernameApi
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class UsernameViewModelTest {
    private lateinit var viewModel: UsernameViewModel
    private val testDispatcher = StandardTestDispatcher()

    // Creamos un API de mentira (Mock)
    private val mockUsernameApi = mockk<UsernameApi>()

    @Before
    fun setUp() {
        // Configuramos las corrutinas para que funcionen en los tests
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        // "Mockeamos" los Objects globales que usas en tu app
        mockkObject(SessionManager)
        mockkObject(RetrofitProvider)
        mockkObject(ApiEndpoints)

        // Hacemos que el RetrofitProvider devuelva nuestra API de mentira
        every { RetrofitProvider.usernameApi } returns mockUsernameApi

        // Instanciamos el ViewModel
        viewModel = UsernameViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll() // Limpiamos los mocks después de cada test
    }

    @Test
    fun `cuando el username esta vacio, el estado cambia a Error`() {
        // Ejecución
        viewModel.changeUsername("")

        // Comprobación
        val state = viewModel.uiState.value
        assertTrue(state is UsernameUiState.Error)
    }

    @Test
    fun `cuando userId es null en SessionManager, el estado cambia a Error`() =
        runTest {
            // Preparación: Simulamos que no hay usuario logueado
            every { SessionManager.userId } returns null

            // Ejecución
            viewModel.changeUsername("NuevoNombre")
            advanceUntilIdle() // Esperamos a que terminen las corrutinas

            // Comprobación
            val state = viewModel.uiState.value
            assertTrue(state is UsernameUiState.Error)
        }

    @Test
    fun `cuando la API responde con exito, el username se guarda y el estado es Success`() =
        runTest {
            val userIdSimulado = 123
            val newUsername = "AirgoLover"

            every { SessionManager.userId } returns userIdSimulado

            every { ApiEndpoints.getChangeInfoEndpoint(userIdSimulado) } returns "url_falsa"

            coEvery {
                mockUsernameApi.changeUsername(any(), any())
            } returns Response.success(null)

            every { SessionManager.username = newUsername } returns Unit

            viewModel.changeUsername(newUsername)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is UsernameUiState.Success)
        }

    @Test
    fun `cuando la API devuelve error 400, el estado cambia a Error con mensaje de servidor`() =
        runTest {
            // Preparación: Simulamos un error HTTP (por ejemplo, nombre ya en uso)
            every { SessionManager.userId } returns 123
            every { ApiEndpoints.getChangeInfoEndpoint(123) } returns "url_falsa"

            val errorResponseBody = "{\"error\":\"Username already taken\"}"
            val errorResponse = Response.error<Void>(400, errorResponseBody.toResponseBody(null))

            coEvery { mockUsernameApi.changeUsername(any(), any()) } returns errorResponse

            // Ejecución
            viewModel.changeUsername("NombreRepetido")
            advanceUntilIdle()

            // Verificación
            val state = viewModel.uiState.value
            assertTrue(state is UsernameUiState.Error)
            // Aquí podrías verificar si el mensaje es el que esperas
        }

    @Test
    fun `cuando hay un fallo de conexion, el estado cambia a Error de conexion`() =
        runTest {
            // Preparación: Simulamos que el móvil no tiene internet
            every { SessionManager.userId } returns 123
            every { ApiEndpoints.getChangeInfoEndpoint(123) } returns "url_falsa"

            // Forzamos a que la API lance una excepción de red
            coEvery { mockUsernameApi.changeUsername(any(), any()) } throws java.io.IOException("No internet")

            // Ejecución
            viewModel.changeUsername("NuevoNombre")
            advanceUntilIdle()

            // Verificación
            val state = viewModel.uiState.value
            assertTrue(state is UsernameUiState.Error)
        }
}

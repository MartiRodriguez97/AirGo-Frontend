package com.airgo.airgoApp.features.settings.account.tebici.domain

import com.airgo.airgoApp.core.network.ApiEndpoints
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.settings.account.tebici.data.TeBiciApi
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class TeBiciViewModelTest {
    private lateinit var viewModel: TeBiciViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val mockTeBiciApi = mockk<TeBiciApi>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(SessionManager)
        mockkObject(RetrofitProvider)
        mockkObject(ApiEndpoints)

        every { RetrofitProvider.teBiciApi } returns mockTeBiciApi
        viewModel = TeBiciViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `cuando actualizamos disponibilidad de bici con exito, el estado es Success`() = runTest {
        // Mocking datos
        every { SessionManager.userId } returns 123
        every { ApiEndpoints.getChangeInfoEndpoint(123) } returns "url_update_bike"

        // Mocking API
        coEvery {
            mockTeBiciApi.updateHasBike(any(), any())
        } returns Response.success(Unit)

        // Mocking SessionManager setter
        every { SessionManager.teBici = any() } returns Unit

        viewModel.updateBikeAvailability(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is TeBiciUiState.Success)
        verify { SessionManager.teBici = true }
    }
}
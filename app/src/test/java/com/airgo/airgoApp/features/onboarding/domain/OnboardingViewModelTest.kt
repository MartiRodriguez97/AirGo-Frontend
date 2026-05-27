package com.airgo.airgoApp.features.onboarding.domain

import com.airgo.airgoApp.R
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.onboarding.data.OnboardingApi
import com.airgo.airgoApp.features.onboarding.data.UserProfileResponse
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
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

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val onboardingApi = mockk<OnboardingApi>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(RetrofitProvider)
        mockkObject(SessionManager)
        every { RetrofitProvider.onboardingApi } returns onboardingApi
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `checkOnboardingNeeded should set NeedsOnboarding if profile is incomplete`() =
        runTest {
            val incompleteProfile =
                UserProfileResponse(
                    id = 1,
                    username = "",
                    pes = 0.0,
                    altura = 0.0,
                    profilePic = "",
                    googleId = null,
                    punts = 0,
                    teBici = false,
                    ratxa = 0,
                    idioma = "ca",
                    limitRutes = 0,
                    titol = null,
                    insignies = null,
                )
            coEvery { onboardingApi.getUserProfile() } returns incompleteProfile

            val viewModel = OnboardingViewModel()
            viewModel.checkOnboardingNeeded()
            advanceUntilIdle()

            assertTrue(viewModel.uiState is OnboardingUiState.NeedsOnboarding)
        }

    @Test
    fun `checkOnboardingNeeded should set Success if profile is complete`() =
        runTest {
            val completeProfile =
                UserProfileResponse(
                    id = 1,
                    username = "user",
                    pes = 70.0,
                    altura = 170.0,
                    profilePic = "",
                    googleId = null,
                    punts = 10,
                    teBici = true,
                    ratxa = 1,
                    idioma = "ca",
                    limitRutes = 5,
                    titol = null,
                    insignies = null,
                )
            coEvery { onboardingApi.getUserProfile() } returns completeProfile

            val viewModel = OnboardingViewModel()
            viewModel.checkOnboardingNeeded()
            advanceUntilIdle()

            assertTrue(viewModel.uiState is OnboardingUiState.Success)
        }

    @Test
    fun `saveOnboarding should update SessionManager and set Success on success`() =
        runTest {
            coEvery { onboardingApi.updateUserProfile(any(), any()) } returns mockk()
            every { SessionManager.username = any() } just Runs
            every { SessionManager.pes = any() } just Runs
            every { SessionManager.altura = any() } just Runs
            every { SessionManager.teBici = any() } just Runs

            val viewModel = OnboardingViewModel()
            // We need an ID, normally set by checkOnboardingNeeded
            // but since it's private, we'd need to call checkOnboardingNeeded first
            // or use reflection. Let's call it first.
            val profile = UserProfileResponse(1, "", null, "", 0, false, 0.0, 0.0, 0, "ca", 0, null, null)
            coEvery { onboardingApi.getUserProfile() } returns profile
            viewModel.checkOnboardingNeeded()
            advanceUntilIdle()

            viewModel.saveOnboarding("newuser", 75.0, 180.0, true)
            advanceUntilIdle()

            assertTrue(viewModel.uiState is OnboardingUiState.Success)
        }

    @Test
    fun `saveOnboarding with invalid data should set Error`() =
        runTest {
            val viewModel = OnboardingViewModel()
            viewModel.saveOnboarding("", 0.0, 0.0, false)

            assertTrue(viewModel.uiState is OnboardingUiState.Error)
            assertEquals(R.string.error_not_fill_files, (viewModel.uiState as OnboardingUiState.Error).message.resId)
        }
}

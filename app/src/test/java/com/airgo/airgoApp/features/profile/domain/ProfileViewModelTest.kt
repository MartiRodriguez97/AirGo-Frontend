package com.airgo.airgoApp.features.profile.domain

import android.content.ContentResolver
import android.content.Context
import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.core.network.UserPreferences
import com.airgo.airgoApp.core.network.UserPreferencesRepository
import com.airgo.airgoApp.core.utils.FileUtils
import com.airgo.airgoApp.features.onboarding.data.UserProfileResponse
import com.airgo.airgoApp.features.profile.data.ProfileApi
import com.airgo.airgoApp.features.profile.data.ProfilePictureResponse
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: ProfileViewModel
    private lateinit var mockApi: ProfileApi

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockApi = mockk()
        mockkObject(RetrofitProvider)
        every { RetrofitProvider.profileApi } returns mockApi

        mockkObject(SessionManager)
        mockkConstructor(UserPreferencesRepository::class)

        // Mock getUserProfile() called in ViewModel init
        val fakeProfile =
            UserProfileResponse(
                id = 1,
                profilePic = "url",
                googleId = null,
                username = "testuser",
                punts = 100,
                teBici = true,
                pes = 70.0,
                altura = 175.0,
                ratxa = 5,
                idioma = "en",
                limitRutes = 10,
                titol = null,
                insignies = null,
            )
        coEvery { mockApi.getUserProfile() } returns fakeProfile
        every { SessionManager.pes = any() } just Runs
        every { SessionManager.altura = any() } just Runs
        every { SessionManager.username = any() } just Runs
        every { SessionManager.userBadges } returns emptyList()

        viewModel = ProfileViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ─── Estat inicial ──────────────────────────────────────────────────────

    @Test
    fun `uiState initial value is Idle`() =
        runTest {
            val state = viewModel.uiState.first()
            assertTrue(state is ProfileUiState.Idle)
        }

    // ─── deleteAccount ───────────────────────────────────────────────────────

    @Test
    fun `deleteAccount success sets DeleteSuccess state`() =
        runTest {
            coEvery { mockApi.deleteAccount() } returns Response.success(null)
            every { SessionManager.clear() } just Runs

            viewModel.deleteAccount()

            val state = viewModel.uiState.first()
            assertTrue(state is ProfileUiState.DeleteSuccess)
            verify { SessionManager.clear() }
        }

    @Test
    fun `deleteAccount HTTP error sets Error state`() =
        runTest {
            coEvery { mockApi.deleteAccount() } returns
                Response.error(403, "Forbidden".toResponseBody())

            viewModel.deleteAccount()

            val state = viewModel.uiState.first()
            assertTrue(state is ProfileUiState.Error)
        }

    @Test
    fun `deleteAccount IOException sets Error state`() =
        runTest {
            coEvery { mockApi.deleteAccount() } throws IOException("Sin red")

            viewModel.deleteAccount()

            val state = viewModel.uiState.first()
            assertTrue(state is ProfileUiState.Error)
        }

    @Test
    fun `deleteAccount sets Loading before request`() =
        runTest {
            val states = mutableListOf<ProfileUiState>()
            coEvery { mockApi.deleteAccount() } coAnswers {
                states.add(viewModel.uiState.first()) // captura durante la llamada
                Response.success(null)
            }
            every { SessionManager.clear() } just Runs

            viewModel.deleteAccount()

            assertTrue(states.any { it is ProfileUiState.Loading })
        }

    // ─── updateProfilePicture ────────────────────────────────────────────────

    @Test
    fun `updateProfilePicture null file sets Error state`() =
        runTest {
            val context = mockk<Context>()
            val contentResolver = mockk<ContentResolver>()
            every { context.contentResolver } returns contentResolver
            every { contentResolver.getType(any()) } returns "image/jpeg"

            mockkObject(FileUtils)
            every { FileUtils.getFileFromUri(any(), any()) } returns null

            viewModel.updateProfilePicture(context, "content://fake/uri")

            val state = viewModel.uiState.first()
            assertTrue(state is ProfileUiState.Error)
        }

    @Test
    fun `updateProfilePicture success sets PictureUpdateSuccess`() =
        runTest {
            val context = mockk<Context>(relaxed = true)
            val contentResolver = mockk<ContentResolver>()
            val fakeFile = mockk<File>(relaxed = true)

            every { context.contentResolver } returns contentResolver
            every { contentResolver.getType(any()) } returns "image/jpeg"
            every { fakeFile.name } returns "photo.jpg"
            every { fakeFile.delete() } returns true

            mockkObject(FileUtils)
            every { FileUtils.getFileFromUri(any(), any()) } returns fakeFile

            val responseBody = ProfilePictureResponse(newPictureUrl = "https://example.com/pic.jpg")
            coEvery { mockApi.updateProfilePicture(any()) } returns Response.success(responseBody)

            every { SessionManager.profilePictureUrl = any() } just Runs
            every { SessionManager.toUserPreferences() } returns mockk<UserPreferences>()
            coEvery { anyConstructed<UserPreferencesRepository>().saveSession(any()) } just Runs

            viewModel.updateProfilePicture(context, "content://fake/uri")

            val state = viewModel.uiState.first()
            assertTrue(state is ProfileUiState.PictureUpdateSuccess)
            verify { SessionManager.profilePictureUrl = "https://example.com/pic.jpg" }
        }

    @Test
    fun `updateProfilePicture success with empty url sets PictureUpdateSuccess`() =
        runTest {
            val context = mockk<Context>(relaxed = true)
            val contentResolver = mockk<ContentResolver>()
            val fakeFile = mockk<File>(relaxed = true)

            every { context.contentResolver } returns contentResolver
            every { contentResolver.getType(any()) } returns "image/jpeg"
            every { fakeFile.name } returns "photo.jpg"
            every { fakeFile.delete() } returns true

            mockkObject(FileUtils)
            every { FileUtils.getFileFromUri(any(), any()) } returns fakeFile

            val responseBody = ProfilePictureResponse(newPictureUrl = "")
            coEvery { mockApi.updateProfilePicture(any()) } returns Response.success(responseBody)
            every { SessionManager.profilePictureUrl = any() } just Runs
            every { SessionManager.toUserPreferences() } returns mockk<UserPreferences>()
            coEvery { anyConstructed<UserPreferencesRepository>().saveSession(any()) } just Runs

            viewModel.updateProfilePicture(context, "content://fake/uri")

            val state = viewModel.uiState.first()
            assertTrue(state is ProfileUiState.PictureUpdateSuccess)
        }

    @Test
    fun `updateProfilePicture HTTP error sets Error`() =
        runTest {
            val context = mockk<Context>()
            val contentResolver = mockk<ContentResolver>()
            val fakeFile = mockk<File>(relaxed = true)

            every { context.contentResolver } returns contentResolver
            every { contentResolver.getType(any()) } returns "image/jpeg"
            every { fakeFile.name } returns "photo.jpg"

            mockkObject(FileUtils)
            every { FileUtils.getFileFromUri(any(), any()) } returns fakeFile

            coEvery { mockApi.updateProfilePicture(any()) } returns
                Response.error(500, "Error".toResponseBody())

            viewModel.updateProfilePicture(context, "content://fake/uri")

            val state = viewModel.uiState.first()
            assertTrue(state is ProfileUiState.Error)
        }

    @Test
    fun `updateProfilePicture IOException sets Error`() =
        runTest {
            val context = mockk<Context>()
            val contentResolver = mockk<ContentResolver>()
            val fakeFile = mockk<File>(relaxed = true)

            every { context.contentResolver } returns contentResolver
            every { contentResolver.getType(any()) } returns "image/jpeg"
            every { fakeFile.name } returns "photo.jpg"

            mockkObject(FileUtils)
            every { FileUtils.getFileFromUri(any(), any()) } returns fakeFile

            coEvery { mockApi.updateProfilePicture(any()) } throws IOException("Sin red")

            viewModel.updateProfilePicture(context, "content://fake/uri")

            val state = viewModel.uiState.first()
            assertTrue(state is ProfileUiState.Error)
        }

    // ─── resetUiState ────────────────────────────────────────────────────────

    @Test
    fun `resetUiState sets Idle`() =
        runTest {
            coEvery { mockApi.deleteAccount() } throws IOException()
            viewModel.deleteAccount()

            viewModel.resetUiState()

            val state = viewModel.uiState.first()
            assertTrue(state is ProfileUiState.Idle)
        }
}

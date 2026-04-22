package com.airgo.airgoApp.features.profile.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ProfileApiTest {
    private lateinit var api: ProfileApi

    @Before
    fun setUp() {
        api = mockk()
    }

    @Test
    fun `deleteAccount returns success response`() =
        runTest {
            coEvery { api.deleteAccount() } returns Response.success(null)

            val result = api.deleteAccount()

            assertTrue(result.isSuccessful)
            assertEquals(200, result.code())
            coVerify(exactly = 1) { api.deleteAccount() }
        }

    @Test
    fun `deleteAccount returns 401 unauthorized`() =
        runTest {
            coEvery { api.deleteAccount() } returns
                Response.error(401, "Unauthorized".toResponseBody())

            val result = api.deleteAccount()

            assertFalse(result.isSuccessful)
            assertEquals(401, result.code())
        }

    @Test
    fun `deleteAccount returns 500 server error`() =
        runTest {
            coEvery { api.deleteAccount() } returns
                Response.error(500, "Internal Server Error".toResponseBody())

            val result = api.deleteAccount()

            assertFalse(result.isSuccessful)
            assertEquals(500, result.code())
        }

    @Test
    fun `updateProfilePicture returns success with url`() =
        runTest {
            val imagePart = mockk<MultipartBody.Part>()
            val responseBody = ProfilePictureResponse(newPictureUrl = "https://cdn.example.com/pic.jpg")

            coEvery { api.updateProfilePicture(imagePart) } returns Response.success(responseBody)

            val result = api.updateProfilePicture(imagePart)

            assertTrue(result.isSuccessful)
            assertEquals("https://cdn.example.com/pic.jpg", result.body()?.newPictureUrl)
            coVerify(exactly = 1) { api.updateProfilePicture(imagePart) }
        }

    @Test
    fun `updateProfilePicture returns 400 bad request`() =
        runTest {
            val imagePart = mockk<MultipartBody.Part>()

            coEvery { api.updateProfilePicture(imagePart) } returns
                Response.error(400, "Bad Request".toResponseBody())

            val result = api.updateProfilePicture(imagePart)

            assertFalse(result.isSuccessful)
            assertEquals(400, result.code())
        }

    @Test
    fun `updateProfilePicture returns null body on success`() =
        runTest {
            val imagePart = mockk<MultipartBody.Part>()

            coEvery { api.updateProfilePicture(imagePart) } returns Response.success(null)

            val result = api.updateProfilePicture(imagePart)

            assertTrue(result.isSuccessful)
            assertNull(result.body())
        }
}

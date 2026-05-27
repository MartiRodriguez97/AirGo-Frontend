package com.airgo.airgoApp.features.social.domain

import com.airgo.airgoApp.core.network.RetrofitProvider
import com.airgo.airgoApp.core.network.SessionManager
import com.airgo.airgoApp.features.social.data.SocialApi
import com.airgo.airgoApp.features.social.friends.data.AmicInfo
import com.airgo.airgoApp.features.social.friends.data.FriendResponse
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SocialViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val socialApi = mockk<SocialApi>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(RetrofitProvider)
        mockkObject(SessionManager)
        every { RetrofitProvider.socialApi } returns socialApi
        every { RetrofitProvider.forumApi } returns mockk()
        every { SessionManager.userId } returns 1
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loadFriends should update state to Success when repository returns data`() =
        runTest {
            val friends =
                listOf(
                    FriendResponse(1, "ACCEPTED", "", AmicInfo(2, "friend1", null)),
                )
            coEvery { socialApi.getFriends(any()) } returns friends

            val viewModel = SocialViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.friendsUiState.value is FriendsUiState.Success)
            assertEquals(friends, (viewModel.friendsUiState.value as FriendsUiState.Success).friends)
        }

    @Test
    fun `loadFriends should update state to Empty when repository returns empty list`() =
        runTest {
            coEvery { socialApi.getFriends(any()) } returns emptyList()

            val viewModel = SocialViewModel()
            advanceUntilIdle()

            assertTrue(viewModel.friendsUiState.value is FriendsUiState.Empty)
        }

    @Test
    fun `onFriendsQueryChange should filter the list`() =
        runTest {
            val friends =
                listOf(
                    FriendResponse(1, "ACCEPTED", "", AmicInfo(2, "alice", null)),
                    FriendResponse(2, "ACCEPTED", "", AmicInfo(3, "bob", null)),
                )
            coEvery { socialApi.getFriends(any()) } returns friends

            val viewModel = SocialViewModel()
            advanceUntilIdle()

            viewModel.onFriendsQueryChange("ali")

            val state = viewModel.friendsUiState.value as FriendsUiState.Success
            assertEquals(1, state.friends.size)
            assertEquals("alice", state.friends[0].amic.username)
        }

    @Test
    fun `deleteFriend should remove friend from state`() =
        runTest {
            val friend1 = FriendResponse(1, "ACCEPTED", "", AmicInfo(2, "alice", null))
            val friend2 = FriendResponse(2, "ACCEPTED", "", AmicInfo(3, "bob", null))
            coEvery { socialApi.getFriends(any()) } returns listOf(friend1, friend2)
            coEvery { socialApi.deleteFriend(2) } returns retrofit2.Response.success(null)

            val viewModel = SocialViewModel()
            advanceUntilIdle()

            viewModel.deleteFriend(2)
            advanceUntilIdle()

            val state = viewModel.friendsUiState.value as FriendsUiState.Success
            assertEquals(1, state.friends.size)
            assertEquals("bob", state.friends[0].amic.username)
        }

    @Test
    fun `getConversationId should return correct ID`() =
        runTest {
            val friend = FriendResponse(1, "ACCEPTED", "", AmicInfo(10, "bob", "url"))
            coEvery { socialApi.getFriends(any()) } returns listOf(friend)
            every { SessionManager.userId } returns 5

            val viewModel = SocialViewModel()
            advanceUntilIdle()

            var resultId = ""
            viewModel.getConversationId(10) { id, _, _, _ ->
                resultId = id
            }

            assertEquals("5-10", resultId)
        }
}

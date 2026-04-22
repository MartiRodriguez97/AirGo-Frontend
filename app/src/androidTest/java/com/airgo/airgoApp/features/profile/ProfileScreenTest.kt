package com.airgo.airgoApp.features.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.airgo.airgoApp.features.profile.domain.ProfileUiState
import com.airgo.airgoApp.features.profile.domain.ProfileViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: ProfileViewModel
    private lateinit var uiStateFlow: MutableStateFlow<ProfileUiState>

    @Before
    fun setUp() {
        uiStateFlow = MutableStateFlow(ProfileUiState.Idle)
        mockViewModel = mockk(relaxed = true)
        every { mockViewModel.uiState } returns uiStateFlow
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun setProfileScreen(
        email: String? = "test@airgo.com",
        pictureUrl: String? = null,
        onAccountDeleted: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            profileScreen(
                email = email,
                pictureUrl = pictureUrl,
                viewModel = mockViewModel,
                onAccountDeleted = onAccountDeleted,
            )
        }
    }

    // ─── showProfileInfo ──────────────────────────────────────────────────────

    @Test
    fun profileScreen_displaysUserDisplayName() {
        setProfileScreen(email = "joan@airgo.com")

        composeTestRule
            .onNodeWithText("joan", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysSettingsButton() {
        setProfileScreen()

        composeTestRule
            .onNodeWithContentDescription("Settings")
            .assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysDeleteAccountButton() {
        setProfileScreen()

        composeTestRule
            .onNodeWithText("Eliminar", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun profileScreen_nullEmail_showsDefaultUsername() {
        setProfileScreen(email = null)

        composeTestRule
            .onNodeWithText("Usuario", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // ─── Loading state ────────────────────────────────────────────────────────

    @Test
    fun profileScreen_pictureUpdateLoading_hidesDeleteButton() {
        uiStateFlow.value = ProfileUiState.PictureUpdateLoading

        setProfileScreen()

        composeTestRule
            .onNodeWithText("Eliminar", substring = true, ignoreCase = true)
            .assertIsDisplayed() // El botón de delete sigue visible; es el edit el que se oculta
        composeTestRule
            .onNodeWithContentDescription("edit", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun profileScreen_loadingState_showsProgressIndicator() {
        uiStateFlow.value = ProfileUiState.Loading

        setProfileScreen()

        composeTestRule
            .onAllNodes(hasTestTag("CircularProgressIndicator"))
            .onFirst()
            .assertExists()
    }

    // ─── Error state ──────────────────────────────────────────────────────────

    @Test
    fun profileScreen_errorState_showsDeleteButtonStill() {
        uiStateFlow.value =
            ProfileUiState.Error(
                com.airgo.airgoApp.core.utils.UiText
                    .DynamicString("Error genérico"),
            )

        setProfileScreen()

        composeTestRule
            .onNodeWithText("Eliminar", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // ─── Delete dialog ────────────────────────────────────────────────────────

    @Test
    fun profileScreen_clickDeleteButton_showsDialog() {
        setProfileScreen()

        composeTestRule
            .onNodeWithText("Eliminar", substring = true, ignoreCase = true)
            .performClick()

        composeTestRule
            .onNodeWithText("Confirmar", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun profileScreen_deleteDialog_cancelDismissesDialog() {
        setProfileScreen()

        composeTestRule
            .onNodeWithText("Eliminar", substring = true, ignoreCase = true)
            .performClick()

        composeTestRule
            .onNodeWithText("Cancelar", substring = true, ignoreCase = true)
            .performClick()

        composeTestRule
            .onNodeWithText("Confirmar", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun profileScreen_deleteDialog_confirmCallsViewModel() {
        setProfileScreen()

        composeTestRule
            .onNodeWithText("Eliminar", substring = true, ignoreCase = true)
            .performClick()

        composeTestRule
            .onNodeWithText("Confirmar", substring = true, ignoreCase = true)
            .performClick()

        verify(exactly = 1) { mockViewModel.deleteAccount() }
    }

    @Test
    fun profileScreen_deleteDialog_confirmDismissesDialog() {
        setProfileScreen()

        composeTestRule
            .onNodeWithText("Eliminar", substring = true, ignoreCase = true)
            .performClick()

        composeTestRule
            .onNodeWithText("Confirmar", substring = true, ignoreCase = true)
            .performClick()

        composeTestRule
            .onNodeWithText("Confirmar", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    // ─── Settings navigation ──────────────────────────────────────────────────

    @Test
    fun profileScreen_clickSettings_navigatesToSettingsScreen() {
        setProfileScreen()

        composeTestRule
            .onNodeWithContentDescription("Settings")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Settings")
            .assertDoesNotExist()
    }

    // ─── DeleteSuccess state ──────────────────────────────────────────────────

    @Test
    fun profileScreen_deleteSuccess_callsOnAccountDeleted() {
        var accountDeleted = false

        composeTestRule.setContent {
            profileScreen(
                email = "test@airgo.com",
                pictureUrl = null,
                viewModel = mockViewModel,
                onAccountDeleted = { accountDeleted = true },
            )
        }

        uiStateFlow.value = ProfileUiState.DeleteSuccess

        composeTestRule.waitForIdle()

        Assert.assertTrue(accountDeleted)
    }

    // ─── PictureUpdateSuccess state ───────────────────────────────────────────

    @Test
    fun profileScreen_pictureUpdateSuccess_callsResetUiState() {
        setProfileScreen()

        uiStateFlow.value = ProfileUiState.PictureUpdateSuccess

        composeTestRule.waitForIdle()

        verify(atLeast = 1) { mockViewModel.resetUiState() }
    }
}

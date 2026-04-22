package com.airgo.airgoApp.features.profile.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileUserInfoCardTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun profileUserInfoCard_displaysDisplayName() {
        composeTestRule.setContent {
            profileUserInfoCard(
                displayName = "Joan Pérez",
                titol = null,
                pictureUrl = null,
                initial = "J",
                isUpdatingPicture = false,
                onEditProfilePictureClick = {},
            )
        }
        composeTestRule
            .onNodeWithText("Joan Pérez")
            .assertIsDisplayed()
    }

    @Test
    fun profileUserInfoCard_withTitol_displaysTitol() {
        composeTestRule.setContent {
            profileUserInfoCard(
                displayName = "Joan",
                titol = "Pilot Expert",
                pictureUrl = null,
                initial = "J",
                isUpdatingPicture = false,
                onEditProfilePictureClick = {},
            )
        }
        composeTestRule
            .onNodeWithText("Pilot Expert")
            .assertIsDisplayed()
    }

    @Test
    fun profileUserInfoCard_nullTitol_showsFallbackText() {
        composeTestRule.setContent {
            profileUserInfoCard(
                displayName = "Joan",
                titol = null,
                pictureUrl = null,
                initial = "J",
                isUpdatingPicture = false,
                onEditProfilePictureClick = {},
            )
        }
        composeTestRule
            .onNodeWithText("Sin título", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun profileUserInfoCard_withPictureUrl_showsLinkedSuccessText() {
        composeTestRule.setContent {
            profileUserInfoCard(
                displayName = "Joan",
                titol = null,
                pictureUrl = "https://example.com/photo.jpg",
                initial = "J",
                isUpdatingPicture = false,
                onEditProfilePictureClick = {},
            )
        }
        // El texto de foto vinculada aparece solo cuando pictureUrl no es blank
        composeTestRule
            .onNodeWithText("foto", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun profileUserInfoCard_nullPictureUrl_doesNotShowLinkedText() {
        composeTestRule.setContent {
            profileUserInfoCard(
                displayName = "Joan",
                titol = null,
                pictureUrl = null,
                initial = "J",
                isUpdatingPicture = false,
                onEditProfilePictureClick = {},
            )
        }
        composeTestRule
            .onNodeWithText("foto", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun profileUserInfoCard_editClick_callsCallback() {
        val onEdit: () -> Unit = mockk(relaxed = true)

        composeTestRule.setContent {
            profileUserInfoCard(
                displayName = "Joan",
                titol = null,
                pictureUrl = null,
                initial = "J",
                isUpdatingPicture = false,
                onEditProfilePictureClick = onEdit,
            )
        }
        composeTestRule
            .onNodeWithContentDescription("edit", substring = true, ignoreCase = true)
            .performClick()

        verify(exactly = 1) { onEdit() }
    }

    @Test
    fun profileUserInfoCard_loading_hidesEditButton() {
        composeTestRule.setContent {
            profileUserInfoCard(
                displayName = "Joan",
                titol = null,
                pictureUrl = null,
                initial = "J",
                isUpdatingPicture = true,
                onEditProfilePictureClick = {},
            )
        }
        composeTestRule
            .onNodeWithContentDescription("edit", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }
}

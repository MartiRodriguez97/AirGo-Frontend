package com.airgo.airgoApp.features.profile.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.airgo.airgoApp.R
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileUserInfoCardTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

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
            .onNodeWithText(context.getString(R.string.profile_no_title), substring = true, ignoreCase = true)
            .assertExists()
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
            .onNodeWithContentDescription(context.getString(R.string.profile_edit_avatar_description), substring = false, ignoreCase = true)
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
            .onNodeWithContentDescription(context.getString(R.string.profile_edit_avatar_description), substring = false, ignoreCase = true)
            .assertDoesNotExist()
    }
}

package com.airgo.airgoApp.features.profile.components

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
class ProfileAvatarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun profileAvatar_nullUrl_showsFallback() {
        composeTestRule.setContent {
            profileAvatar(
                pictureUrl = null,
                initial = "A",
                isLoading = false,
                onEditClick = {},
            )
        }
        composeTestRule
            .onNodeWithTag("profile_avatar_fallback")
            .assertIsDisplayed()
    }

    @Test
    fun profileAvatar_emptyUrl_showsFallback() {
        composeTestRule.setContent {
            profileAvatar(
                pictureUrl = "",
                initial = "B",
                isLoading = false,
                onEditClick = {},
            )
        }
        composeTestRule
            .onNodeWithTag("profile_avatar_fallback")
            .assertIsDisplayed()
    }

    @Test
    fun profileAvatar_fallback_displaysInitial() {
        composeTestRule.setContent {
            profileAvatar(
                pictureUrl = null,
                initial = "Z",
                isLoading = false,
                onEditClick = {},
            )
        }
        composeTestRule
            .onNodeWithTag("profile_avatar_fallback")
            .onChildren()
            .filterToOne(hasText("Z"))
            .assertIsDisplayed()
    }

    @Test
    fun profileAvatar_notLoading_showsEditButton() {
        composeTestRule.setContent {
            profileAvatar(
                pictureUrl = null,
                initial = "A",
                isLoading = false,
                onEditClick = {},
            )
        }
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.profile_edit_avatar_description), ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun profileAvatar_loading_hidesEditButton() {
        composeTestRule.setContent {
            profileAvatar(
                pictureUrl = null,
                initial = "A",
                isLoading = true,
                onEditClick = {},
            )
        }
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.profile_edit_avatar_description), ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun profileAvatar_editButton_click_firesCallback() {
        val onEditClick: () -> Unit = mockk(relaxed = true)

        composeTestRule.setContent {
            profileAvatar(
                pictureUrl = null,
                initial = "A",
                isLoading = false,
                onEditClick = onEditClick,
            )
        }
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.profile_edit_avatar_description), ignoreCase = true)
            .performClick()

        verify(exactly = 1) { onEditClick() }
    }

    @Test
    fun profileAvatar_loading_showsOverlay() {
        composeTestRule.setContent {
            profileAvatar(
                pictureUrl = null,
                initial = "A",
                isLoading = true,
                onEditClick = {},
            )
        }
        composeTestRule
            .onAllNodes(isRoot())
            .onFirst()
            .assert(hasAnyDescendant(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)))
    }
}

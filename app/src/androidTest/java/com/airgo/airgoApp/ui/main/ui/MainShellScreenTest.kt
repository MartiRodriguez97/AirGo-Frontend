package com.airgo.airgoApp.ui.main.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.airgo.airgoApp.core.theme.airGoTheme
import com.airgo.airgoApp.features.shell.mainShellScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainShellScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsFiveBottomTabs() {
        composeTestRule.setContent {
            airGoTheme {
                mainShellScreen(
                    email = "airgo@example.com",
                    pictureUrl = null,
                )
            }
        }

        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Forum").assertIsDisplayed()
        composeTestRule.onNodeWithText("Actividad").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mensajes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Perfil").assertIsDisplayed()
    }

    @Test
    fun switchesContentWhenSelectingAnotherTab() {
        composeTestRule.setContent {
            airGoTheme {
                mainShellScreen(
                    email = "airgo@example.com",
                    pictureUrl = "https://example.com/avatar.png",
                )
            }
        }

        composeTestRule.onNodeWithText("Mensajes").performClick()
        composeTestRule.onNodeWithText("Consulta tus conversaciones y avisos").assertIsDisplayed()

        composeTestRule.onNodeWithText("Perfil").performClick()
        composeTestRule.onNodeWithText("airgo@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Foto vinculada correctamente").assertIsDisplayed()
    }

    @Test
    fun showsProfileImageNodeWhenPictureUrlExists() {
        composeTestRule.setContent {
            airGoTheme {
                mainShellScreen(
                    email = "airgo@example.com",
                    pictureUrl = "https://example.com/avatar.png",
                )
            }
        }

        composeTestRule.onNodeWithText("Perfil").performClick()
        composeTestRule.onNodeWithTag("profile_avatar_image").assertIsDisplayed()
        composeTestRule.onNodeWithText("Foto vinculada correctamente").assertIsDisplayed()
    }

    @Test
    fun showsFallbackAvatarWhenPictureUrlIsNull() {
        composeTestRule.setContent {
            airGoTheme {
                mainShellScreen(
                    email = "airgo@example.com",
                    pictureUrl = null,
                )
            }
        }

        composeTestRule.onNodeWithText("Perfil").performClick()
        composeTestRule.onNodeWithTag("profile_avatar_fallback").assertIsDisplayed()
        val imageNodes = composeTestRule.onAllNodesWithTag("profile_avatar_image").fetchSemanticsNodes()
        assertTrue(imageNodes.isEmpty())
    }
}
